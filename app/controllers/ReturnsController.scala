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

import java.net.URLDecoder
import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import config.AppConfig
import models.VatReturn
import models.errors.UnexpectedStatusError
import models.viewModels.VatReturnViewModel
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{EnrolmentsAuthService, ReturnsService, VatApiService}

@Singleton
class ReturnsController @Inject()(val messagesApi: MessagesApi,
                                  val enrolmentsAuthService: EnrolmentsAuthService,
                                  returnsService: ReturnsService,
                                  vatApiService: VatApiService,
                                  implicit val appConfig: AppConfig)
  extends AuthorisedController {

  def vatReturnDetails(periodKey: String, isReturnsPageRequest: Boolean = true): Action[AnyContent] = authorisedAction {
    implicit request =>
      implicit user =>
        //TODO: use period key for service request
        val decodedPeriodKey: String = URLDecoder.decode(periodKey, "UTF-8")
        val vatReturnCall = returnsService.getVatReturnDetails(user, LocalDate.now(), LocalDate.now())
        val entityNameCall = vatApiService.getEntityName(user)

        for {
          vatReturnResult <- vatReturnCall
          customerInfo <- entityNameCall
        } yield {
          vatReturnResult match {
            case Right(vatReturn) => Ok(views.html.returns.vatReturnDetails(constructViewModel(customerInfo, vatReturn, isReturnsPageRequest)))
            case Left(UnexpectedStatusError(404)) => NotFound(views.html.errors.notFound())
            case Left(_) => InternalServerError(views.html.errors.serverError())
          }
        }
  }

  def vatPaymentReturnDetails(periodKey: String): Action[AnyContent] = vatReturnDetails(periodKey, isReturnsPageRequest = false)

  private[controllers] def constructViewModel(entityName: Option[String], vatReturn: VatReturn, isReturnsPageRequest: Boolean): VatReturnViewModel = {
    VatReturnViewModel(
      entityName = entityName,
      periodFrom = LocalDate.parse("2018-01-01"),
      periodTo = LocalDate.parse("2018-03-31"),
      dueDate = LocalDate.parse("2018-05-07"),
      dateSubmitted = LocalDate.parse("2018-04-02"),
      boxOne = vatReturn.vatDueSales,
      boxTwo = vatReturn.vatDueAcquisitions,
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
