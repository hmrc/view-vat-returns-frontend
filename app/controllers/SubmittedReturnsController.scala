/*
 * Copyright 2020 HM Revenue & Customs
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
import common.SessionKeys
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.viewModels.{ReturnObligationsViewModel, VatReturnsViewModel}
import models.{ServiceResponse, User}
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
        migrationDate <- getMigratedToETMPDate
        obligationsResult <- getReturnObligations(migrationDate)
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

  private[controllers] def getReturnObligations(migrationDate: Option[LocalDate])
                                               (implicit user: User,
                                                hc: HeaderCarrier): Future[ServiceResponse[VatReturnsViewModel]] = {

    val years = getValidYears(migrationDate)

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
          val migratedWithin15Months = customerMigratedWithin15M(migrationDate)

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

  private[controllers] def getMigratedToETMPDate(implicit request: Request[_], user: User): Future[Option[LocalDate]] =
    request.session.get(SessionKeys.migrationToETMP) match {
      case Some(date) if date.nonEmpty => Future.successful(Some(LocalDate.parse(date)))
      case Some(_) => Future.successful(None)
      case None => subscriptionService.getUserDetails(user.vrn) map {
        case Some(details) => details.customerMigratedToETMPDate.map(LocalDate.parse)
        case None => None
      }
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
