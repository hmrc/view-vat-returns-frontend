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

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import config.AppConfig
import models.errors.UnexpectedStatusError
import models.payments.Payment
import models.viewModels.VatReturnViewModel
import models.{VatReturn, VatReturnObligation}
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{EnrolmentsAuthService, ReturnsService, SubscriptionService}

@Singleton
class ReturnsController @Inject()(val messagesApi: MessagesApi,
                                  val enrolmentsAuthService: EnrolmentsAuthService,
                                  returnsService: ReturnsService,
                                  subscriptionService: SubscriptionService,
                                  implicit val appConfig: AppConfig)
  extends AuthorisedController {

  def vatReturnDetails(periodKey: String, yearEnd: Int, isReturnsPageRequest: Boolean = true): Action[AnyContent] = authorisedAction {
    implicit request =>
      implicit user =>

        val vatReturnCall = returnsService.getVatReturnDetails(user, periodKey)
        val entityNameCall = subscriptionService.getEntityName(user)
        val financialDataCall = returnsService.getPayment(user, periodKey)
        val obligationCall = returnsService.getObligationWithMatchingPeriodKey(user, yearEnd, periodKey)

        for {
          vatReturnResult <- vatReturnCall
          customerInfo <- entityNameCall
          payment <- financialDataCall
          obligation <- obligationCall
        } yield {
          (vatReturnResult, obligation) match {
            case (Right(vatReturn), Some(ob)) =>
              val viewModel = constructViewModel(customerInfo, ob, payment, vatReturn, isReturnsPageRequest)
              Ok(views.html.returns.vatReturnDetails(viewModel))
            case (Right(_), None) => NotFound(views.html.errors.notFound())
            case (Left(UnexpectedStatusError(404)), _) => NotFound(views.html.errors.notFound())
            case (Left(_), _) => InternalServerError(views.html.errors.serverError())
          }
        }
  }

  def vatPaymentReturnDetails(periodKey: String, yearEnd: Int): Action[AnyContent] = vatReturnDetails(periodKey, yearEnd, isReturnsPageRequest = false)

  private[controllers] def constructViewModel(entityName: Option[String],
                                              obligation: VatReturnObligation,
                                              payment: Option[Payment],
                                              vatReturn: VatReturn,
                                              isReturnsPageRequest: Boolean): VatReturnViewModel = {

    // TODO: update this value to reflect partial payments
    val amountToShow: BigDecimal = payment.fold(vatReturn.netVatDue) {
      _.outstandingAmount
    }

    VatReturnViewModel(
      entityName = entityName,
      periodFrom = obligation.start,
      periodTo = obligation.end,
      dueDate = obligation.due,
      dateSubmitted = obligation.received.getOrElse(LocalDate.now()),
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
