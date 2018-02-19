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

import javax.inject.{Inject, Singleton}

import config.AppConfig
import models.errors.UnexpectedStatusError
import models.payments.Payment
import models.viewModels.VatReturnViewModel
import models.{ReturnsControllerData, VatReturn, VatReturnObligation}
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{EnrolmentsAuthService, ReturnsService, SubscriptionService}

import scala.concurrent.Future

@Singleton
class ReturnsController @Inject()(val messagesApi: MessagesApi,
                                  val enrolmentsAuthService: EnrolmentsAuthService,
                                  returnsService: ReturnsService,
                                  subscriptionService: SubscriptionService,
                                  implicit val appConfig: AppConfig)
  extends AuthorisedController {

  def vatReturn(year: Int, periodKey: String): Action[AnyContent] = authorisedAction {
    implicit request =>
      implicit user =>
        val isReturnsPageRequest = true

        val vatReturnCall = returnsService.getVatReturn(user, periodKey)
        val entityNameCall = subscriptionService.getEntityName(user)
        val financialDataCall = returnsService.getPayment(user, periodKey)
        val obligationCall = returnsService.getObligationWithMatchingPeriodKey(user, year, periodKey)

        for {
          vatReturnResult <- vatReturnCall
          customerInfo <- entityNameCall
          payment <- financialDataCall
          obligation <- obligationCall
        } yield {
          val data = ReturnsControllerData(vatReturnResult, customerInfo, payment, obligation)
          renderResult(data, isReturnsPageRequest)
        }
  }

  def vatReturnViaPayments(periodKey: String): Action[AnyContent] = authorisedAction {
    implicit request =>
      implicit user =>
        val isReturnsPageRequest = false

        val vatReturnCall = returnsService.getVatReturn(user, periodKey)
        val entityNameCall = subscriptionService.getEntityName(user)
        val financialDataCall = returnsService.getPayment(user, periodKey)
        def obligationCall(payment: Option[Payment]) = {
          // Had to use Option.empty because the fold didn't like None
          payment.fold(Future.successful(Option.empty[VatReturnObligation])) { p =>
            returnsService.getObligationWithMatchingPeriodKey(user, p.end.getYear, periodKey)
          }
        }

        for {
          vatReturnResult <- vatReturnCall
          customerInfo <- entityNameCall
          payment <- financialDataCall
          obligation <- obligationCall(payment)
        } yield {
          val data = ReturnsControllerData(vatReturnResult, customerInfo, payment, obligation)
          renderResult(data, isReturnsPageRequest)
        }
  }

  private[controllers] def renderResult(pageData: ReturnsControllerData, isReturnsPageRequest: Boolean)(implicit req: Request[_]) = {
    (pageData.vatReturnResult, pageData.obligation) match {
      case (Right(vatReturn), Some(ob)) =>
        val viewModel = constructViewModel(pageData.customerInfo, ob, pageData.payment, vatReturn, isReturnsPageRequest)
        Ok(views.html.returns.vatReturnDetails(viewModel))
      case (Right(_), None) => NotFound(views.html.errors.notFound())
      case (Left(UnexpectedStatusError(404)), _) => NotFound(views.html.errors.notFound())
      case (Left(_), _) => InternalServerError(views.html.errors.serverError())
    }
  }

  private[controllers] def constructViewModel(entityName: Option[String],
                                              obligation: VatReturnObligation,
                                              payment: Option[Payment],
                                              vatReturn: VatReturn,
                                              isReturnsPageRequest: Boolean): VatReturnViewModel = {

    // TODO: update this value to reflect partial payments
    val amountToShow: BigDecimal = vatReturn.netVatDue

    val periodFrom = payment.fold(obligation.start)(_.start)

    val periodTo = payment.fold(obligation.end)(_.end)

    val dueDate = payment.fold(obligation.due)(_.due)


    VatReturnViewModel(
      entityName = entityName,
      periodFrom = periodFrom,
      periodTo = periodTo,
      dueDate = dueDate,
      dateSubmitted = obligation.received.get,
      boxOne = vatReturn.vatDueSales,
      boxTwo = vatReturn.vatDueAcquisitions,
      outstandingAmount = amountToShow,
      boxThree = vatReturn.totalVatDue,
      boxFour = vatReturn.vatReclaimedCurrPeriod,
      boxFive = vatReturn.netVatDue,
      boxSix = vatReturn.totalValueSalesExVAT,
      boxSeven = vatReturn.totalValuePurchasesExVAT,
      boxEight = vatReturn.totalValueGoodsSuppliedExVAT,
      boxNine = vatReturn.totalAcquisitionsExVAT,
      showReturnsBreadcrumb = isReturnsPageRequest
    )
  }
}
