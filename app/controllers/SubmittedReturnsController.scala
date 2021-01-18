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

import java.time.{LocalDate, Period}
import audit.AuditingService
import audit.models.ViewSubmittedVatObligationsAuditModel
import config.AppConfig

import javax.inject.{Inject, Singleton}
import models.viewModels.{ReturnObligationsViewModel, VatReturnsViewModel}
import models.{MigrationDateModel, ServiceResponse, User}
import models.errors.ObligationError
import models.Obligation.Status.Fulfilled
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.HtmlFormat
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.LoggerUtil.logWarn
import views.html.errors.SubmittedReturnsErrorView
import views.html.returns.SubmittedReturnsView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmittedReturnsController @Inject()(mcc: MessagesControllerComponents,
                                           returnsService: ReturnsService,
                                           authorisedController: AuthorisedController,
                                           dateService: DateService,
                                           serviceInfoService: ServiceInfoService,
                                           subscriptionService: SubscriptionService,
                                           submittedReturnsView: SubmittedReturnsView,
                                           submittedReturnsErrorView: SubmittedReturnsErrorView)
                                          (implicit appConfig: AppConfig,
                                           auditService: AuditingService,
                                           ec: ExecutionContext) extends FrontendController(mcc) {

  def redirect(year: Int): Action[AnyContent] = authorisedController.authorisedAction({ _ =>
    _ =>
      Future(MovedPermanently(controllers.routes.SubmittedReturnsController.submittedReturns().url))
  }, ignoreMandatedStatus = true)

  def currentYear: Int = dateService.now().getYear

  def submittedReturns: Action[AnyContent] = authorisedController.authorisedAction({ implicit request =>
    implicit user =>
      for {
        serviceInfoContent <- if (user.isAgent) Future.successful(HtmlFormat.empty) else serviceInfoService.getServiceInfoPartial
        migrationDates <- getMigrationDates
        obligationsResult <- getReturnObligations(migrationDates)
      } yield {
        obligationsResult match {
          case Right(model) =>
            Ok(submittedReturnsView(model, serviceInfoContent))
          case Left(error) =>
            logWarn("[ReturnObligationsController][submittedReturns] error: " + error.toString)
            InternalServerError(submittedReturnsErrorView(user))
        }
      }
  }, ignoreMandatedStatus = true)

  private[controllers] def getValidYears(migrationDate: Option[LocalDate]): Seq[Int] =
    migrationDate match {
      case Some(date) if date.getYear == currentYear => Seq(currentYear)
      case Some(date) if date.getYear == currentYear - 1 => Seq(currentYear, currentYear - 1)
      case _ => Seq(currentYear, currentYear - 1, currentYear - 2)
    }

  private[controllers] def getReturnObligations(migrationDatesModel: MigrationDateModel)
                                               (implicit user: User,
                                                hc: HeaderCarrier): Future[ServiceResponse[VatReturnsViewModel]] = {

    val years = getValidYears(migrationDatesModel.migratedToETMPDate)

    for {
      obligationsResult <- Future.sequence(years.map { year =>
        returnsService.getReturnObligationsForYear(user.vrn, year, Fulfilled)
      })
    } yield {
      obligationsResult match {

        case result if result.exists(_.isLeft) =>
          Left(ObligationError)

        case result =>

          val obligations = result.flatMap(_.right.toSeq).flatMap(_.obligations)
          val migratedWithin15Months = customerMigratedWithin15M(migrationDatesModel.hybridToFullDate)

          auditService.extendedAudit(
            ViewSubmittedVatObligationsAuditModel(user, obligations),
            routes.SubmittedReturnsController.submittedReturns().url
          )

          Right(VatReturnsViewModel(
            years,
            obligations.map(obligation =>
              ReturnObligationsViewModel(
                obligation.periodFrom,
                obligation.periodTo,
                obligation.periodKey
              )
            ),
            user.hasNonMtdVat && migratedWithin15Months,
            user.vrn
          ))
      }
    }
  }

  private[controllers] def getMigrationDates(implicit request: Request[_], user: User): Future[MigrationDateModel] =
      subscriptionService.getUserDetails(user.vrn) map {
        case Some(details) =>
          MigrationDateModel(details.customerMigratedToETMPDate.map(LocalDate.parse), details.extractDate.map(LocalDate.parse))
        case None =>
          MigrationDateModel(None, None)
      }

  private[controllers] def customerMigratedWithin15M(migrationDate: Option[LocalDate]): Boolean =
    migrationDate match {
      case Some(date) =>
        val prevReturnsMonthLimit = 14
        val monthsSinceMigration = Math.abs(Period.between(dateService.now(), date).toTotalMonths)
        0 to prevReturnsMonthLimit contains monthsSinceMigration
      case None => false
    }
}
