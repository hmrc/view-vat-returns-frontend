/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import java.time.LocalDate

import audit.AuditingService
import audit.models.ViewOpenVatObligationsAuditModel
import common.SessionKeys
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.viewModels.ReturnDeadlineViewModel
import models._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.{Html, HtmlFormat}
import services.{DateService, EnrolmentsAuthService, ReturnsService, ServiceInfoService}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import utils.LoggerUtil.logWarn

import scala.concurrent.Future

@Singleton
class ReturnDeadlinesController @Inject()(val messagesApi: MessagesApi,
                                          enrolmentsAuthService: EnrolmentsAuthService,
                                          returnsService: ReturnsService,
                                          authorisedController: AuthorisedController,
                                          dateService: DateService,
                                          serviceInfoService: ServiceInfoService,
                                          implicit val appConfig: AppConfig,
                                          auditService: AuditingService)
  extends FrontendController with I18nSupport {


  private[controllers] def serviceInfoCall()(implicit user: User, req: Request[_]): Future[Html] = {
    if (user.isAgent) Future.successful(HtmlFormat.empty) else serviceInfoService.getServiceInfoPartial
  }

  private[controllers] def toReturnDeadlineViewModel(obligation: VatReturnObligation, date: LocalDate): ReturnDeadlineViewModel = {
    ReturnDeadlineViewModel(
      obligation.due,
      obligation.periodFrom,
      obligation.periodTo,
      obligation.due.isBefore(date),
      obligation.periodKey
    )
  }

  def returnDeadlines(): Action[AnyContent] = authorisedController.authorisedAction { implicit request =>

    implicit user =>
      val openObligations = returnsService.getOpenReturnObligations(user.vrn)
      val currentDate = dateService.now()

      openObligations.flatMap {
        case Right(VatReturnObligations(obligations)) =>
          serviceInfoCall().flatMap { serviceInfoContent =>
            auditService.extendedAudit(
              ViewOpenVatObligationsAuditModel(user, obligations),
              routes.ReturnDeadlinesController.returnDeadlines().url
            )
            if (obligations.isEmpty) {
              noUpcomingObligationsAction(serviceInfoContent, currentDate)
            } else {
              upcomingObligationsAction(obligations.map(toReturnDeadlineViewModel(_, currentDate)), serviceInfoContent)
            }
          }
        case Left(error) =>
          logWarn("[ReturnObligationsController][returnDeadlines] error: " + error.toString)
          Future.successful(InternalServerError(views.html.errors.technicalProblem()))
      }
  }

  private[controllers] def noUpcomingObligationsAction(serviceInfoContent: Html, currentDate: LocalDate)
                                                      (implicit request: Request[AnyContent],
                                                      user: User): Future[Result] = {
    returnsService.getFulfilledObligations(user.vrn, currentDate).map {
      case Right(VatReturnObligations(Seq())) => Ok(views.html.returns.noUpcomingReturnDeadlines(None, serviceInfoContent))
      case Right(VatReturnObligations(obligations)) =>
        val lastFulfilledObligation: VatReturnObligation = returnsService.getLastObligation(obligations)
        Ok(views.html.returns.noUpcomingReturnDeadlines(Some(toReturnDeadlineViewModel(lastFulfilledObligation, currentDate)), serviceInfoContent))
      case Left(error) =>
        logWarn("[ReturnObligationsController][fulfilledObligationsAction] error: " + error.toString)
        InternalServerError(views.html.errors.technicalProblem())
    }
  }

  private[controllers] def upcomingObligationsAction(obligations: Seq[ReturnDeadlineViewModel],
                                                     serviceInfoContent: Html)
                                                    (implicit user: User, request: Request[AnyContent]): Future[Result] = {

    def view(mandationStatus: String) = mandationStatus match {
      case NonMtdfb.mandationStatus => views.html.returns.optOutReturnDeadlines(obligations, dateService.now(), serviceInfoContent)
      case _ => views.html.returns.returnDeadlines(obligations, serviceInfoContent)
    }

    if (appConfig.features.submitReturnFeatures()) {
      request.session.get(SessionKeys.mtdVatMandationStatus) match {
        case Some(status) => Future.successful(Ok(view(status)))
        case None =>
          returnsService.getMandationStatus(user.vrn) map {
            case Right(MandationStatus(status)) =>
              Ok(view(status)).addingToSession(SessionKeys.mtdVatMandationStatus -> status)
            case error =>
              logWarn(s"[ReturnObligationsController][handleMandationStatus] - getMandationStatus returned an Error: $error")
              InternalServerError(views.html.errors.technicalProblem())
          }
      }
    } else {
      Future.successful(Ok(views.html.returns.returnDeadlines(obligations, serviceInfoContent)))
    }
  }
}
