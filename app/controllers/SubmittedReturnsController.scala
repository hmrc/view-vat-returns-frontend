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
import common.SessionKeys
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.viewModels.{ReturnDeadlineViewModel, ReturnObligationsViewModel, VatReturnsViewModel}
import models._
import models.errors.ServiceError
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.twirl.api.{Html, HtmlFormat}
import services.{DateService, EnrolmentsAuthService, ReturnsService, ServiceInfoService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class SubmittedReturnsController @Inject()(val messagesApi: MessagesApi,
                                           enrolmentsAuthService: EnrolmentsAuthService,
                                           returnsService: ReturnsService,
                                           authorisedController: AuthorisedController,
                                           dateService: DateService,
                                           serviceInfoService: ServiceInfoService,
                                           implicit val appConfig: AppConfig,
                                           auditService: AuditingService)
  extends FrontendController with I18nSupport {

  def submittedReturns(year: Int): Action[AnyContent] = authorisedController.authorisedAction { implicit request =>
    implicit user =>
      if (isValidSearchYear(year)) {
        for {
          obligationsResult <- getReturnObligations(user, year, Obligation.Status.Fulfilled)
          serviceInfoContent <- if(user.isAgent) Future.successful(HtmlFormat.empty) else serviceInfoService.getServiceInfoPartial
        } yield {
         obligationsResult match {
            case Right(model) =>
              Ok(views.html.returns.submittedReturns(model, serviceInfoContent))
            case Left(error) =>
              Logger.warn("[ReturnObligationsController][submittedReturns] error: " + error.toString)
              InternalServerError(views.html.errors.submittedReturnsError(user))
          }
        }
      } else {
        Future.successful(NotFound(views.html.errors.notFound()))
      }
  }



  private[controllers] def isValidSearchYear(year: Int, upperBound: Int = dateService.now().getYear) = {
    year <= upperBound && year >= upperBound - 2
  }

  private[controllers] def getPreviousReturnYears(user: User, status: Obligation.Status.Value, currentYear: Int)
                                                 (implicit hc: HeaderCarrier): Future[ServiceResponse[Seq[Int]]] = {

    val currentYearMinusOne = currentYear - 1

    returnsService.getReturnObligationsForYear(user, currentYearMinusOne, status) map {
      case Right(VatReturnObligations(obligations)) =>
        if (obligations.nonEmpty) {
          Right(Seq[Int](currentYear, currentYearMinusOne))
        }
        else {
          Right(Seq[Int](currentYear))
        }
      case Left(error) =>
        Logger.warn("[ReturnObligationsController][getPreviousReturnYears] error: " + error.toString)
        Left(error)
    }
  }

  private[controllers] def getReturnYears(user: User, status: Obligation.Status.Value)(implicit hc: HeaderCarrier): Future[ServiceResponse[Seq[Int]]] = {

    val currentYear = dateService.now().getYear
    val currentYearMinusTwo = currentYear - 2

    if (currentYear > 2019) {
      returnsService.getReturnObligationsForYear(user, currentYearMinusTwo, status) flatMap {
        case Right(VatReturnObligations(obligations)) =>
          if (obligations.nonEmpty) {
            Future.successful(Right(Seq[Int](currentYear, currentYear - 1, currentYearMinusTwo)))
          } else {
            getPreviousReturnYears(user, status, currentYear)
          }
        case Left(error) =>
          Logger.warn("[ReturnObligationsController][getReturnYears] error: " + error.toString)
          Future.successful(Left(error))
      }
    }
    else {
      getPreviousReturnYears(user, status, currentYear)
    }
  }

  private[controllers] def getReturnObligations(user: User, selectedYear: Int, status: Obligation.Status.Value)
                                               (implicit hc: HeaderCarrier): Future[Either[ServiceError, VatReturnsViewModel]] = {

    getReturnYears(user, status) flatMap {
      case Right(years) =>

        if (years.contains(selectedYear)) {
          returnsService.getReturnObligationsForYear(user, selectedYear, status) map {
            case Right(VatReturnObligations(obligations)) =>
              auditService.extendedAudit(
                ViewSubmittedVatObligationsAuditModel(user, obligations),
                routes.SubmittedReturnsController.submittedReturns(selectedYear).url
              )

              Right(VatReturnsViewModel(
                years,
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
            case Left(error) =>
              Logger.warn(s"[ReturnObligationsController][getReturnObligations] error: ${error.toString}")
              Left(error)
          }
        }
        else {
          Future.successful(Right(VatReturnsViewModel(
            years,
            selectedYear,
            Seq(),
            user.hasNonMtdVat,
            user.vrn
          )))
        }
      case Left(error) =>
        Logger.warn(s"[ReturnObligationsController][getReturnObligations] error: ${error.toString}")
        Future.successful(Left(error))
    }
  }

}
