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
import audit.models.AuditModel
import connectors.VatSubscriptionConnector
import models.ServiceResponse
import models.errors.PaymentSetupError
import models.payments.PaymentDetailsModel
import play.api.http.Status
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{EnrolmentsAuthService, PaymentsService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class MakePaymentControllerSpec extends ControllerBaseSpec {

  private trait MakePaymentDetailsTest {
    val authResult: Future[_] = Future.successful(Enrolments(Set(
        Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "")
    )))

    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockVatSubscriptionConnector: VatSubscriptionConnector = mock[VatSubscriptionConnector]
    val mockPaymentsService: PaymentsService = mock[PaymentsService]
    val mockAuditService: AuditingService = mock[AuditingService]
    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)

    val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequestToPOSTWithSession(
      ("amountInPence", "10000"),
      ("taxPeriodMonth", "02"),
      ("taxPeriodYear", "18"),
      ("periodKey", "#001")
    )

    val redirectUrl = "http://www.google.com"
    val paymentsServiceResponse: ServiceResponse[String] = Right(redirectUrl)
    val paymentsServiceCall: Boolean = true

    def setup(): Any = {
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(authResult)

      (mockAuditService.audit(_: AuditModel, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .stubs(*, *, *, *)
        .returns({})

      if(paymentsServiceCall) {
        (mockPaymentsService.setupPaymentsJourney(_: PaymentDetailsModel)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(paymentsServiceResponse))
      }
    }

    def target: MakePaymentController = {
      setup()
      new MakePaymentController(messages, mockEnrolmentsAuthService, mockPaymentsService, mockConfig, mockAuditService)
    }
  }

  "Calling the makePayment action" when {

    "the user is logged in and the Payments journey is set up correctly" should {

      "redirect the user to the URL returned from the service" in new MakePaymentDetailsTest {
        val result: Future[Result] = target.makePayment()(request)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(redirectUrl)
      }

      "the user is logged in and the Payments journey is not set up correctly" should {

        "return an internal server error" in new MakePaymentDetailsTest {
          override val paymentsServiceResponse: ServiceResponse[String] = Left(PaymentSetupError)
          val result: Future[Result] = target.makePayment()(request)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }

      "the user is logged in and the form does not bind successfully" should {

        "return an internal server error" in new MakePaymentDetailsTest {
          override val paymentsServiceCall: Boolean = false
          override val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequestToPOSTWithSession(
            ("bad", "data"),
            ("wrong", "fields")
          )
          val result: Future[Result] = target.makePayment()(request)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }

    "the user is not logged in" should {

      "return 401 (Unauthorised)" in new MakePaymentDetailsTest {
        override val paymentsServiceCall = false
        override val authResult: Future[Nothing] = Future.failed(MissingBearerToken())
        val result: Future[Result] = target.makePayment()(request)

        status(result) shouldBe Status.UNAUTHORIZED
      }
    }

    "the user is not authenticated" should {

      "return 403 (Forbidden)" in new MakePaymentDetailsTest {
        override val paymentsServiceCall = false
        override val authResult: Future[Nothing] = Future.failed(InsufficientEnrolments())
        val result: Future[Result] = target.makePayment()(request)

        status(result) shouldBe Status.FORBIDDEN
      }
    }
  }

  "Calling the .payment function" when {

    def paymentModel(payFromNineBox: Boolean): PaymentDetailsModel = PaymentDetailsModel(
      taxType = "vat",
      taxReference = "123456789",
      amountInPence = 10000,
      taxPeriodMonth = 2,
      taxPeriodYear = 2018,
      returnUrl = "payments-return-url",
      backUrl = if(payFromNineBox) "/" else controllers.routes.ReturnsController.vatReturn(2018, "#001").url,
      periodKey = "#001"
    )

    "the data from the request model shows that the user was on the 'Check what you owe' page" should {

      "return a model with the correct back URL" in new MakePaymentDetailsTest {
        override def setup(): Unit = ()
        val model: PaymentDetailsModel = paymentModel(payFromNineBox = true)
        val expectedResult = "www.app.com/vat-through-software/vat-returns/submitted/2018/%23001"
        val result: String = target.payment(model, "").backUrl

        result shouldBe expectedResult
      }
    }

    "the data from the request model shows that the user was on the '9 box' page" should {

      "return a model with the correct back URL" in new MakePaymentDetailsTest {
        override def setup(): Unit = ()
        val model: PaymentDetailsModel = paymentModel(payFromNineBox = false)
        val expectedResult = "www.app.com/vat-through-software/vat-returns/%23001"
        val result: String = target.payment(model, "").backUrl

        result shouldBe expectedResult
      }
    }
  }
}

