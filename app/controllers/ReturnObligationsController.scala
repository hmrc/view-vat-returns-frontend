/*
 * Copyright 2018 HM Revenue & Customs
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

import audit.AuditingService
import audit.models.{ViewSubmittedVatObligationsAuditModel, ViewOpenVatObligationsAuditModel}
import config.AppConfig
import connectors.httpParsers.ResponseHttpParsers.HttpGetResult
import javax.inject.Inject

import models.viewModels.{ReturnDeadlineViewModel, ReturnObligationsViewModel, VatReturnsViewModel}
import models.{Obligation, User, VatReturnObligation, VatReturnObligations}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{DateService, EnrolmentsAuthService, ReturnsService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ReturnObligationsController @Inject()(val messagesApi: MessagesApi,
                                            val enrolmentsAuthService: EnrolmentsAuthService,
                                            returnsService: ReturnsService,
                                            dateService: DateService,
                                            implicit val appConfig: AppConfig,
                                            auditService: AuditingService)
  extends AuthorisedController {

  def submittedReturns(year: Int): Action[AnyContent] = authorisedAction { implicit request =>
    implicit user =>
      if (isValidSearchYear(year)) {
        getReturnObligations(user, year, Obligation.Status.Fulfilled).map {
          case Right(model) => Ok(views.html.returns.submittedReturns(model))
          case Left(_) => InternalServerError(views.html.errors.submittedReturnsError(user))
        }
      } else {
        Future.successful(NotFound(views.html.errors.notFound()))
      }
  }

  def returnDeadlines(): Action[AnyContent] = authorisedAction { implicit request =>
    implicit user =>
      val currentDate = dateService.now()
      val openObligationsCall = returnsService.getReturnObligationsForYear(user, currentDate.getYear, Obligation.Status.Outstanding)
      lazy val closedObligationsCall = returnsService.getFulfilledObligations(currentDate)

      openObligationsCall.flatMap {
        case Right(VatReturnObligations(Seq())) => closedObligationsCall.map(fulfilledObligations => noOpenObligationsAction(fulfilledObligations))
        case Right(VatReturnObligations(obligations)) =>
          val deadlines = obligations.map(ob =>
            ReturnDeadlineViewModel(ob.due, ob.start, ob.end, ob.due.isBefore(currentDate))
          )

          auditService.extendedAudit(
            ViewOpenVatObligationsAuditModel(user, obligations),
            routes.ReturnObligationsController.returnDeadlines().url
          )

          Future.successful(Ok(views.html.returns.returnDeadlines(deadlines)))
        case Left(_) => Future.successful(InternalServerError(views.html.errors.technicalProblem()))
      }
  }

  private def noOpenObligationsAction(obligationsResult: HttpGetResult[VatReturnObligations])(implicit request: Request[AnyContent]): Result = {
    obligationsResult match {
      case Right(VatReturnObligations(Seq())) => Ok(views.html.returns.noUpcomingReturnDeadlines(None))
      case Right(obligations) =>
        val lastFulfilledObligation: VatReturnObligation = returnsService.getLastObligation(obligations)
        Ok(views.html.returns.noUpcomingReturnDeadlines(Some(ReturnDeadlineViewModel(
          lastFulfilledObligation.due,
          lastFulfilledObligation.start,
          lastFulfilledObligation.end
        ))))
      case Left(_) => InternalServerError(views.html.errors.technicalProblem())
    }
  }

  private[controllers] def isValidSearchYear(year: Int, upperBound: Int = dateService.now().getYear) = {
    year <= upperBound && year >= upperBound - 1
  }

  private[controllers] def getReturnObligations(user: User, selectedYear: Int, status: Obligation.Status.Value)
                                               (implicit hc: HeaderCarrier): Future[HttpGetResult[VatReturnsViewModel]] = {

    val returnYears: Seq[Int] = Seq[Int](2018)

    returnsService.getReturnObligationsForYear(user, selectedYear, status).map {
      case Right(VatReturnObligations(obligations)) =>
        auditService.extendedAudit(
          ViewSubmittedVatObligationsAuditModel(user, obligations),
          routes.ReturnObligationsController.submittedReturns(selectedYear).url
        )

        Right(VatReturnsViewModel(
          returnYears,
          selectedYear,
          obligations.map(obligation =>
            ReturnObligationsViewModel(
              obligation.start,
              obligation.end,
              obligation.periodKey
            )
          ),
          user.hasNonMtdVat,
          user.vrn
        ))
      case Left(error) => Left(error)
    }
  }
}
