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
import audit.models.ViewVatReturnAuditModel
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.customer.CustomerDetail
import models.errors.NotFoundError
import models.payments.Payment
import models.viewModels.VatReturnViewModel
import models._
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
        val isReturnsPageRequest = true
        val vatReturnCall = returnsService.getVatReturn(user, periodKey)
        val entityNameCall = subscriptionService.getUserDetails(user)
        val financialDataCall = returnsService.getPayment(user, periodKey)
        val obligationCall = returnsService.getObligationWithMatchingPeriodKey(user, year, periodKey)
        val hasDirectDebitCall = returnsService.getDirectDebitStatus(user.vrn)

        for {
          vatReturn <- vatReturnCall
          customerInfo <- entityNameCall
          payment <- financialDataCall
          obligation <- obligationCall
          directDebit <- hasDirectDebitCall
        } yield {
          val data = ReturnsControllerData(vatReturn, customerInfo, payment, obligation, directDebit)
          renderResult(data, isReturnsPageRequest)
        }
  }

  def vatReturnViaPayments(periodKey: String): Action[AnyContent] = authorisedAction {
    implicit request =>
      implicit user =>
        val isReturnsPageRequest = false
        val vatReturnCall = returnsService.getVatReturn(user, periodKey)
        val entityNameCall = subscriptionService.getUserDetails(user)
        val financialDataCall = returnsService.getPayment(user, periodKey)
        val hasDirectDebitCall = returnsService.getDirectDebitStatus(user.vrn)

        def obligationCall(payment: Option[Payment]) = {
          payment.fold(Future.successful(Option.empty[VatReturnObligation])) { p =>
            returnsService.getObligationWithMatchingPeriodKey(user, p.end.getYear, periodKey)
          }
        }

        for {
          vatReturnResult <- vatReturnCall
          customerInfo <- entityNameCall
          payment <- financialDataCall
          obligation <- obligationCall(payment)
          directDebit <- hasDirectDebitCall
        } yield {
          val data = ReturnsControllerData(vatReturnResult, customerInfo, payment, obligation, directDebit)
          renderResult(data, isReturnsPageRequest)
        }
  }

  private[controllers] def renderResult(pageData: ReturnsControllerData, isReturnsPageRequest: Boolean)(implicit req: Request[_], user: User) = {
    (pageData.vatReturnResult, pageData.obligation, pageData.payment) match {
      case (Right(vatReturn), Some(ob), payment) =>
        val returnDetails = returnsService.constructReturnDetailsModel(vatReturn, payment)
        val directDebit = getDirectDebitStatus(pageData.hasDirectDebit)
        val viewModel = constructViewModel(pageData.customerInfo, ob, returnDetails, isReturnsPageRequest, directDebit)
        auditEvent(isReturnsPageRequest, viewModel)
        Ok(views.html.returns.vatReturnDetails(viewModel))
      case (Left(NotFoundError), _, _) =>
        NotFound(views.html.errors.notFound())
      case (Right(_), None, _) =>
        Logger.warn("[ReturnsController][renderResult] error: render required a valid obligation but none was returned")
        InternalServerError(views.html.errors.technicalProblem())
      case _ =>
        Logger.warn("[ReturnsController][renderResult] error: Unknown error")
        InternalServerError(views.html.errors.technicalProblem())
    }
  }

  private[controllers] def getDirectDebitStatus(response: ServiceResponse[Boolean]): Boolean = {
    response match {
      case Right(directDebit) => directDebit
      case Left(error) =>
        Logger.warn("[ReturnsController][getDirectDebitStatus] error: " + error.toString)
        false
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
                                              directDebitStatus: Boolean): VatReturnViewModel = {

    val amountToShow: BigDecimal = returnDetails.vatReturn.netVatDue

    VatReturnViewModel(
      entityName = customerDetail.fold(Option.empty[String])(detail => Some(detail.entityName)),
      periodFrom = obligation.start,
      periodTo = obligation.end,
      dueDate = obligation.due,
      outstandingAmount = amountToShow,
      dateSubmitted = obligation.received.get,
      vatReturnDetails = returnDetails,
      showReturnsBreadcrumb = isReturnsPageRequest,
      currentYear = dateService.now().getYear,
      hasFlatRateScheme = customerDetail.fold(false)(_.hasFlatRateScheme),
      hasDirectDebit = directDebitStatus
    )
  }
}