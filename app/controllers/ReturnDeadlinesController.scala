/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.predicate.DDInterruptPredicate
import javax.inject.{Inject, Singleton}
import models.viewModels.ReturnDeadlineViewModel
import models._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, MessagesRequest, Request, Result}
import play.twirl.api.{Html, HtmlFormat}
import services.{DateService, ReturnsService, ServiceInfoService, SubscriptionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.LoggerUtil.logWarn
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
                                          optOutReturnDeadlinesView: OptOutReturnDeadlinesView,
                                          DDInterrupt: DDInterruptPredicate)
                                         (implicit appConfig: AppConfig,
                                          auditService: AuditingService,
                                          ec: ExecutionContext) extends FrontendController(mcc) {


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
      DDInterrupt.interruptCheck { _ =>
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
            Future.successful(errorHandler.showInternalServerError)
        }
      }
  }

  private[controllers] def noUpcomingObligationsAction(serviceInfoContent: Html, currentDate: LocalDate)
                                                      (implicit request: MessagesRequest[AnyContent],
                                                       user: User): Future[Result] = {
    returnsService.getFulfilledObligations(user.vrn, currentDate).map {
      case Right(VatReturnObligations(Seq())) => Ok(noUpcomingReturnDeadlinesView(None, serviceInfoContent))
      case Right(VatReturnObligations(obligations)) =>
        val lastFulfilledObligation: VatReturnObligation = returnsService.getLastObligation(obligations)
        Ok(noUpcomingReturnDeadlinesView(Some(toReturnDeadlineViewModel(lastFulfilledObligation, currentDate)), serviceInfoContent))
      case Left(error) =>
        logWarn("[ReturnObligationsController][fulfilledObligationsAction] error: " + error.toString)
        errorHandler.showInternalServerError
    }
  }

  private[controllers] def upcomingObligationsAction(obligations: Seq[ReturnDeadlineViewModel],
                                                     serviceInfoContent: Html)
                                                    (implicit user: User,
                                                     request: MessagesRequest[AnyContent]): Future[Result] = {

    val submitStatuses : List[String] = List(nonMTDfB, nonDigital, mtdfbExempt)

    def view(mandationStatus: String) = mandationStatus match {
      case status if submitStatuses.contains(status) =>
        optOutReturnDeadlinesView(obligations, dateService.now(), serviceInfoContent)
      case _ =>
        returnDeadlinesView(obligations, serviceInfoContent)
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
