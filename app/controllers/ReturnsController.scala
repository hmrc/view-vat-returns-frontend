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
import models.{ReturnsControllerData, VatReturnDetails, VatReturnObligation}
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{DateService, EnrolmentsAuthService, ReturnsService, SubscriptionService}
import scala.concurrent.Future

@Singleton
class ReturnsController @Inject()(val messagesApi: MessagesApi,
                                  val enrolmentsAuthService: EnrolmentsAuthService,
                                  returnsService: ReturnsService,
                                  subscriptionService: SubscriptionService,
                                  dateService: DateService,
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
          vatReturn <- vatReturnCall
          customerInfo <- entityNameCall
          payment <- financialDataCall
          obligation <- obligationCall
        } yield {
          val data = ReturnsControllerData(vatReturn, customerInfo, payment, obligation)
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
    (pageData.vatReturnResult, pageData.obligation, pageData.payment) match {
      case (Right(vatReturn), Some(ob), payment) =>
        val returnDetails = returnsService.constructReturnDetailsModel(vatReturn, payment)
        val viewModel = constructViewModel(pageData.customerInfo, ob, returnDetails, isReturnsPageRequest)
        if (appConfig.features.allowNineBox()) {
          Ok(views.html.returns.vatReturnDetails(viewModel))
        } else {
          NotFound(views.html.errors.notFound())
        }
      case (Left(UnexpectedStatusError("404", _)), _, _) => NotFound(views.html.errors.notFound())
      case _ => InternalServerError(views.html.errors.technicalProblem())
    }
  }

  private[controllers] def constructViewModel(entityName: Option[String],
                                              obligation: VatReturnObligation,
                                              returnDetails: VatReturnDetails,
                                              isReturnsPageRequest: Boolean): VatReturnViewModel = {

    // TODO: update this value to reflect partial payments
    val amountToShow: BigDecimal = returnDetails.vatReturn.netVatDue

    VatReturnViewModel(
      entityName = entityName,
      periodFrom = obligation.start,
      periodTo = obligation.end,
      dueDate = obligation.due,
      outstandingAmount = amountToShow,
      dateSubmitted = obligation.received.get,
      vatReturnDetails = returnDetails,
      showReturnsBreadcrumb = isReturnsPageRequest,
      currentYear = dateService.now().getYear
    )
  }
}
