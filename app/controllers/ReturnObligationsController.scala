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

import audit.AuditingService
import audit.models.{ViewOpenVatObligationsAuditModel, ViewSubmittedVatObligationsAuditModel}
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.viewModels.{ReturnDeadlineViewModel, ReturnObligationsViewModel, VatReturnsViewModel}
import models._
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{DateService, EnrolmentsAuthService, ReturnsService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
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
          case Left(error) =>
            Logger.warn("[ReturnObligationsController][submittedReturns] error: " + error.toString)
            InternalServerError(views.html.errors.submittedReturnsError(user))
        }
      } else {
        Future.successful(NotFound(views.html.errors.notFound()))
      }
  }

  def returnDeadlines(): Action[AnyContent] = authorisedAction { implicit request =>
    implicit user =>
      val currentDate = dateService.now()
      val openObligationsCall = returnsService.getOpenReturnObligations(user)

      openObligationsCall.flatMap {
        case Right(VatReturnObligations(obligations)) =>
          auditService.extendedAudit(
            ViewOpenVatObligationsAuditModel(user, obligations),
            routes.ReturnObligationsController.returnDeadlines().url
          )
          if (obligations.isEmpty) {
            returnsService.getFulfilledObligations(currentDate).map(fulfilledObligations => fulfilledObligationsAction(fulfilledObligations))
          } else {
            val deadlines = obligations.map(obligation =>
              ReturnDeadlineViewModel(obligation.due, obligation.start, obligation.end, obligation.due.isBefore(currentDate), obligation.periodKey)
            )
            Future.successful(Ok(views.html.returns.returnDeadlines(deadlines)))
          }
        case Left(error) =>
          Logger.warn("[ReturnObligationsController][returnDeadlines] error: " + error.toString)
          Future.successful(InternalServerError(views.html.errors.technicalProblem()))
      }
  }

  private[controllers] def fulfilledObligationsAction(obligationsResult: ServiceResponse[VatReturnObligations])
                                                     (implicit request: Request[AnyContent]): Result = {
    obligationsResult match {
      case Right(VatReturnObligations(Seq())) => Ok(views.html.returns.noUpcomingReturnDeadlines(None))
      case Right(VatReturnObligations(obligations)) =>
        val lastFulfilledObligation: VatReturnObligation = returnsService.getLastObligation(obligations)
        Ok(views.html.returns.noUpcomingReturnDeadlines(Some(ReturnDeadlineViewModel(
          due = lastFulfilledObligation.due,
          start = lastFulfilledObligation.start,
          end = lastFulfilledObligation.end,
          periodKey = lastFulfilledObligation.periodKey
        ))))
      case Left(error) =>
        Logger.warn("[ReturnObligationsController][fulfilledObligationsAction] error: " + error.toString)
        InternalServerError(views.html.errors.technicalProblem())
    }
  }

  private[controllers] def isValidSearchYear(year: Int, upperBound: Int = dateService.now().getYear) = {
    year <= upperBound && year >= upperBound - 2
  }

  private[controllers] def getReturnsYearsPre2020(user: User, status: Obligation.Status.Value, currentYear: Int)
                                                 (implicit hc: HeaderCarrier): Future[ServiceResponse[Seq[Int]]] = {

    returnsService.getReturnObligationsForYear(user, currentYear - 1, status) map {
      case Right(VatReturnObligations(obligations)) =>
        if (obligations.nonEmpty) {
          Right(Seq[Int](currentYear, currentYear - 1))
        }
        else {
          Right(Seq[Int](currentYear))
        }
      case Left(error) =>
        Logger.warn("[ReturnObligationsController][getReturnObligations] error: " + error.toString)
        Left(error)
    }
  }


  private[controllers] def getReturnYears(user: User, status: Obligation.Status.Value)(implicit hc: HeaderCarrier): Future[ServiceResponse[Seq[Int]]] = {

    val currentYear = dateService.now().getYear

    if (currentYear > 2019) {
      returnsService.getReturnObligationsForYear(user, currentYear - 2, status) flatMap {
        case Right(VatReturnObligations(obligations)) =>
          if (obligations.nonEmpty) {
            Future.successful(Right(Seq[Int](currentYear, currentYear - 1, currentYear - 2)))
          } else {
            getReturnsYearsPre2020(user, status, currentYear)
          }
        case Left(error) =>
          Logger.warn("[ReturnObligationsController][getReturnObligations] error: " + error.toString)
          Future.successful(Left(error))
      }
    }
    else {
      getReturnsYearsPre2020(user, status, currentYear)
    }
  }

  private[controllers] def getReturnObligations(user: User, selectedYear: Int, status: Obligation.Status.Value)
                                               (implicit hc: HeaderCarrier): Future[ServiceResponse[VatReturnsViewModel]] = {


    for {
      yearsForTabs <- getReturnYears(user, status)
      obligation <- returnsService.getReturnObligationsForYear(user, selectedYear, status)
    } yield (yearsForTabs, obligation) match {
      case (Right(years), Right(obs)) =>
        auditService.extendedAudit(
          ViewSubmittedVatObligationsAuditModel(user, obs.obligations),
          routes.ReturnObligationsController.submittedReturns(selectedYear).url
        )

//        (years.length > 1, user.hasNonMtdVat) match {
//          case (true, false) => {}
//          case (true, true) => {}
//        }


        Right(VatReturnsViewModel(
          years,
          selectedYear,
          obs.obligations.map(obligation =>
            ReturnObligationsViewModel(
              obligation.start,
              obligation.end,
              obligation.periodKey
            )
          ),
          user.hasNonMtdVat,
          user.vrn
        ))

      case (Left(error), _) =>
        Logger.warn(s"[ReturnObligationsController][getReturnObligations] error: $error.toString")
        Left(error)

      case (_, Left(error)) =>
        Logger.warn(s"[ReturnObligationsController][getReturnObligations] error: $error.toString")
        Left(error)

    }
  }
}
