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
import audit.models.ViewVatReturnAuditModel
import common.SessionKeys
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models._
import models.customer.CustomerDetail
import models.errors.NotFoundError
import models.payments.Payment
import models.viewModels.VatReturnViewModel
import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{DateService, EnrolmentsAuthService, ReturnsService, SubscriptionService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class ReturnsController @Inject()(val messagesApi: MessagesApi,
                                  val enrolmentsAuthService: EnrolmentsAuthService,
                                  returnsService: ReturnsService,
                                  subscriptionService: SubscriptionService,
                                  dateService: DateService,
                                  implicit val appConfig: AppConfig,
                                  auditService: AuditingService)
  extends AuthorisedController {

  def vatReturn(year: Int, periodKey: String): Action[AnyContent] = authorisedAction {
    implicit request =>
      implicit user =>
        if(validPeriodKey(periodKey)) {
          val isReturnsPageRequest = true
          val vatReturnCall = returnsService.getVatReturn(user, periodKey)
          val entityNameCall = subscriptionService.getUserDetails(user)
          val obligationCall = returnsService.getObligationWithMatchingPeriodKey(user, year, periodKey)

          def financialDataCall(customerInfo: Option[CustomerDetail]): Future[Option[Payment]] = {
            val isHybridUser = customerInfo.fold(false)(_.isPartialMigration)
            if (isHybridUser) Future.successful(None) else returnsService.getPayment(user, periodKey, Some(year))
          }

          (for {
            vatReturn <- vatReturnCall
            customerInfo <- entityNameCall
            payment <- financialDataCall(customerInfo)
            obligation <- obligationCall
          } yield ReturnsControllerData(vatReturn, customerInfo, payment, obligation))
            .flatMap(pageData => renderResult(pageData, isReturnsPageRequest))

        } else {
          Logger.warn(s"[ReturnsController][vatReturn] - The given period key was invalid - `$periodKey`")
          Future.successful(NotFound(views.html.errors.notFound()))
        }
  }

  def vatReturnViaPayments(periodKey: String): Action[AnyContent] = authorisedAction {
    implicit request =>
      implicit user =>
        if(validPeriodKey(periodKey)) {
          val isReturnsPageRequest = false
          val vatReturnCall = returnsService.getVatReturn(user, periodKey)
          val entityNameCall = subscriptionService.getUserDetails(user)
          val financialDataCall = returnsService.getPayment(user, periodKey)

          def obligationCall(payment: Option[Payment]) = {
            payment.fold(Future.successful(Option.empty[VatReturnObligation])) { p =>
              returnsService.getObligationWithMatchingPeriodKey(user, p.end.getYear, periodKey)
            }
          }

          (for {
            vatReturnResult <- vatReturnCall
            customerInfo <- entityNameCall
            payment <- financialDataCall
            obligation <- obligationCall(payment)
          } yield ReturnsControllerData(vatReturnResult, customerInfo, payment, obligation))
            .flatMap(pageData => renderResult(pageData, isReturnsPageRequest))

        } else {
          Logger.warn(s"[ReturnsController][vatReturnViaPayments] - The given period key was invalid - `$periodKey`")
          Future.successful(NotFound(views.html.errors.notFound()))
        }
  }

  private def handleMandationStatus(customerDetail: Option[CustomerDetail],
                                    obligation: VatReturnObligation,
                                    returnDetails: VatReturnDetails,
                                    isReturnsPageRequest: Boolean)
                                   (implicit user: User, request: Request[AnyContent]): Future[Result] = {

    def viewModel(isOptedOutUser: Boolean) = constructViewModel(
      customerDetail,
      obligation,
      returnDetails,
      isReturnsPageRequest,
      isOptedOutUser
    )

    if(appConfig.features.submitReturnFeatures()) {
      request.session.get(SessionKeys.mtdVatMandationStatus) match {
        case Some(status) =>
          val model = viewModel(status == NonMtdfb.mandationStatus)
          auditEvent(isReturnsPageRequest, model)
          Future.successful(Ok(views.html.returns.vatReturnDetails(model)))
        case None =>
          returnsService.getMandationStatus(user.vrn) map {
            case Right(MandationStatus(status)) =>
              val model = viewModel(status == NonMtdfb.mandationStatus)
              auditEvent(isReturnsPageRequest, model)
              Ok(views.html.returns.vatReturnDetails(model)).addingToSession(SessionKeys.mtdVatMandationStatus -> status)
            case error =>
              Logger.warn(s"[ReturnsController][handleMandationStatus] - getMandationStatus returned an Error: $error")
              InternalServerError(views.html.errors.technicalProblem())
        }
      }
    } else {
      val model = viewModel(false)
      auditEvent(isReturnsPageRequest, model)
      Future.successful(Ok(views.html.returns.vatReturnDetails(model)))
    }
  }

  private[controllers] def renderResult(pageData: ReturnsControllerData, isReturnsPageRequest: Boolean)
                                       (implicit req: Request[AnyContent], user: User): Future[Result] = {
    (pageData.vatReturnResult, pageData.obligation, pageData.payment) match {
      case (Right(vatReturn), Some(ob), payment) =>
        val returnDetails = returnsService.constructReturnDetailsModel(vatReturn, payment)
        handleMandationStatus(pageData.customerInfo, ob, returnDetails, isReturnsPageRequest)
      case (Left(NotFoundError), _, _) =>
        Future.successful(NotFound(views.html.errors.notFound()))
      case (Right(_), None, _) =>
        Logger.warn("[ReturnsController][renderResult] error: render required a valid obligation but none was returned")
        Future.successful(InternalServerError(views.html.errors.technicalProblem()))
      case _ =>
        Logger.warn("[ReturnsController][renderResult] error: Unknown error")
        Future.successful(InternalServerError(views.html.errors.technicalProblem()))
    }
  }

  private def auditEvent(isReturnsPageRequest: Boolean, data: VatReturnViewModel)(implicit user: User, hc: HeaderCarrier): Unit = {

    val periodKey = data.vatReturnDetails.vatReturn.periodKey

    val auditPath = if (isReturnsPageRequest) {
      routes.ReturnsController.vatReturn(data.periodFrom.getYear, periodKey).url
    }
    else {
      routes.ReturnsController.vatReturnViaPayments(periodKey).url
    }

    auditService.audit(ViewVatReturnAuditModel(user, data), auditPath)
  }

  private[controllers] def constructViewModel(customerDetail: Option[CustomerDetail],
                                              obligation: VatReturnObligation,
                                              returnDetails: VatReturnDetails,
                                              isReturnsPageRequest: Boolean,
                                              isOptedOutUser: Boolean): VatReturnViewModel = {

    val amountToShow: BigDecimal = returnDetails.vatReturn.netVatDue

    VatReturnViewModel(
      entityName = customerDetail.fold(Option.empty[String])(detail => Some(detail.entityName)),
      periodFrom = obligation.start,
      periodTo = obligation.end,
      dueDate = obligation.due,
      returnTotal = amountToShow,
      dateSubmitted = obligation.received.get,
      vatReturnDetails = returnDetails,
      showReturnsBreadcrumb = isReturnsPageRequest,
      currentYear = dateService.now().getYear,
      hasFlatRateScheme = customerDetail.fold(false)(_.hasFlatRateScheme),
      isOptOutMtdVatUser = isOptedOutUser,
      isHybridUser = customerDetail.fold(false)(_.isPartialMigration)
    )
  }

  private[controllers] def validPeriodKey(periodKey: String): Boolean = {
    val periodKeyRegex = """^([0-9 A-Z]{4})$|^(#[0-9]{3})$"""
    periodKey.matches(periodKeyRegex)
  }
}
