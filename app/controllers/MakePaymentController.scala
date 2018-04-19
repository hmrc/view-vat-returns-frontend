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
import audit.models.PaymentAuditModel
import config.AppConfig
import forms.MakePaymentForm
import javax.inject.{Inject, Singleton}
import models.payments.PaymentDetailsModel
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{EnrolmentsAuthService, PaymentsService}

import scala.concurrent.Future

@Singleton
class MakePaymentController @Inject()(val messagesApi: MessagesApi,
                                      val enrolmentsAuthService: EnrolmentsAuthService,
                                      paymentsService: PaymentsService,
                                      implicit val appConfig: AppConfig,
                                      auditService: AuditingService)
  extends AuthorisedController with I18nSupport {

  private[controllers] def payment(data: PaymentDetailsModel, vrn: String): PaymentDetailsModel =
    data.copy(
    taxType = "vat",
    taxReference = vrn,
    returnUrl = appConfig.paymentsReturnUrl,
    taxPeriodYear = data.taxPeriodYear,
    backUrl = appConfig.selfHost + controllers.routes.ReturnsController.vatReturn(data.taxPeriodYear, data.periodKey).url
  )

  def makePayment(): Action[AnyContent] = authorisedAction { implicit request =>
    user =>

      MakePaymentForm.form.bindFromRequest().fold(
        _ => { // failed to bind model
          Logger.warn("[MakePaymentsController].[makePayment] invalid payment data")
          Future.successful(
            InternalServerError(
              views.html.errors.standardError(
                appConfig, Messages("paymentHandOffErrorHeading"),
                Messages("paymentHandOffErrorHeading"),
                Messages("paymentHandOffErrorMessage")
              )
            )
          )
        },
        paymentDetail => {
          val details = payment(paymentDetail, user.vrn)
          paymentsService.setupPaymentsJourney(details).map { url =>
            auditService.audit(
              PaymentAuditModel(user, details, url),
              routes.MakePaymentController.makePayment().url
            )
            Redirect(url)
          }
        }
      )
  }
}
