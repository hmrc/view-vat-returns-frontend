/*
 * Copyright 2024 HM Revenue & Customs
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
import common.MandationStatuses._
import common.SessionKeys
import config.{AppConfig, ServiceErrorHandler}
import javax.inject.{Inject, Singleton}
import models._
import models.viewModels.ReturnDeadlineViewModel
import play.api.mvc._
import play.twirl.api.Html
import services.{DateService, ReturnsService, ServiceInfoService, SubscriptionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.LoggerUtil
import views.html.returns.{NoUpcomingReturnDeadlinesView, OptOutReturnDeadlinesView, ReturnDeadlinesView}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnDeadlinesController @Inject()(mcc: MessagesControllerComponents,
                                          returnsService: ReturnsService,
                                          subscriptionService: SubscriptionService,
                                          authorisedController: AuthorisedController,
                                          dateService: DateService,
                                          serviceInfoService: ServiceInfoService,
                                          errorHandler: ServiceErrorHandler,
                                          noUpcomingReturnDeadlinesView: NoUpcomingReturnDeadlinesView,
                                          returnDeadlinesView: ReturnDeadlinesView,
                                          optOutReturnDeadlinesView: OptOutReturnDeadlinesView)
                                         (implicit appConfig: AppConfig,
                                          auditService: AuditingService,
                                          ec: ExecutionContext) extends FrontendController(mcc) with LoggerUtil {

  private[controllers] def toReturnDeadlineViewModel(obligation: VatReturnObligation, date: LocalDate): ReturnDeadlineViewModel = {
    ReturnDeadlineViewModel(
      obligation.due,
      obligation.periodFrom,
      obligation.periodTo,
      obligation.due.isBefore(date),
      obligation.periodKey
    )
  }

  def returnDeadlines: Action[AnyContent] = authorisedController.authorisedAction { implicit request =>
    implicit user =>

      val openObligations = returnsService.getOpenReturnObligations(user.vrn)
      val currentDate = dateService.now()

      openObligations.flatMap {
        case Right(VatReturnObligations(obligations)) =>
          serviceInfoService.getServiceInfoPartial.flatMap { serviceInfoContent =>
            auditService.extendedAudit(
              ViewOpenVatObligationsAuditModel(user, obligations),
              routes.ReturnDeadlinesController.returnDeadlines.url
            )
            if (obligations.isEmpty) {
              noUpcomingObligationsAction(serviceInfoContent, currentDate)
            } else {
              upcomingObligationsAction(obligations.distinct.map(toReturnDeadlineViewModel(_, currentDate)), serviceInfoContent)
            }
          }
        case Left(error) =>
          logger.warn("[ReturnObligationsController][returnDeadlines] error: " + error.toString)
          Future.successful(errorHandler.showInternalServerError)
      }
  }

  private[controllers] def noUpcomingObligationsAction(serviceInfoContent: Html, currentDate: LocalDate)
                                                      (implicit request: MessagesRequest[AnyContent],
                                                       user: User): Future[Result] = {

    val clientName = request.session.get(SessionKeys.clientName)
    val mandationStatus = request.session.get(SessionKeys.mtdVatMandationStatus).getOrElse("")

    returnsService.getFulfilledObligations(user.vrn, currentDate).map {
      case Right(VatReturnObligations(Seq())) => Ok(noUpcomingReturnDeadlinesView(None, serviceInfoContent, clientName, mandationStatus))
      case Right(VatReturnObligations(obligations)) =>
        val lastFulfilledObligation: VatReturnObligation = returnsService.getLastObligation(obligations)
        Ok(noUpcomingReturnDeadlinesView(Some(toReturnDeadlineViewModel(lastFulfilledObligation, currentDate)),
          serviceInfoContent, clientName, mandationStatus))
      case Left(error) =>
        logger.warn("[ReturnObligationsController][fulfilledObligationsAction] error: " + error.toString)
        errorHandler.showInternalServerError
    }
  }

  private[controllers] def upcomingObligationsAction(obligations: Seq[ReturnDeadlineViewModel],
                                                     serviceInfoContent: Html)
                                                    (implicit user: User,
                                                     request: MessagesRequest[AnyContent]): Future[Result] = {

    val submitStatuses : List[String] = List(nonMTDfB, nonDigital, mtdfbExempt)
    val clientName = request.session.get(SessionKeys.clientName)

    def view(mandationStatus: String) = mandationStatus match {
      case status if submitStatuses.contains(status) =>
        optOutReturnDeadlinesView(obligations, dateService.now(), serviceInfoContent, mandationStatus)
      case _ =>
        returnDeadlinesView(obligations, serviceInfoContent, clientName)
    }

    request.session.get(SessionKeys.mtdVatMandationStatus) match {
      case Some(status) => Future.successful(Ok(view(status)))
      case None =>
        subscriptionService.getUserDetails(user.vrn) map {
          case Some(details) =>
            Ok(view(details.mandationStatus)).addingToSession(SessionKeys.mtdVatMandationStatus -> details.mandationStatus)
          case None =>
            errorHandler.showInternalServerError
        }
    }
  }
}
