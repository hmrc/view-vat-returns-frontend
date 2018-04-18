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
import models.payments.PaymentDetailsModel
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{EnrolmentsAuthService, PaymentsService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class MakePaymentControllerSpec extends ControllerBaseSpec {

  val expectedPayment = PaymentDetailsModel(
    taxType = "vat",
    taxReference = "123456789",
    amountInPence = 10000,
    taxPeriodMonth = 2,
    taxPeriodYear = 2018,
    returnUrl = "payments-return-url",
    backUrl = controllers.routes.ReturnsController.vatReturn(2018, "#001").url,
    periodKey = "#001"
  )

  private trait MakePaymentDetailsTest {
    val authResult: Future[_] =
      Future.successful(Enrolments(Set(
        Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "")
      )))

    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockVatSubscriptionConnector: VatSubscriptionConnector = mock[VatSubscriptionConnector]
    val mockPaymentsService: PaymentsService = mock[PaymentsService]
    val mockAuditService: AuditingService = mock[AuditingService]

    def setup(): Any = {
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(authResult)

      (mockAuditService.audit(_: AuditModel, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .stubs(*, *, *, *)
        .returns({})
    }

    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)

    def target: MakePaymentController = {
      setup()
      new MakePaymentController(messages, mockEnrolmentsAuthService, mockPaymentsService, mockConfig, mockAuditService)
    }
  }

  "Calling the makePayment action" when {

    "the user is logged in" should {
      "have redirect to url returned from service" in new MakePaymentDetailsTest {

        override def setup(): Any = {
          super.setup()

          (mockPaymentsService.setupPaymentsJourney(_: PaymentDetailsModel)(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *)
            .returns(Future.successful(expectedRedirectUrl))
        }

        val expectedRedirectUrl = "http://www.google.com"

        private lazy val request = fakeRequestToPOSTWithSession(
          ("amountInPence", "10000"),
          ("taxPeriodMonth", "02"),
          ("taxPeriodYear", "18"),
          ("periodKey", "#001")
        )

        lazy val result: Future[Result] = target.makePayment()(request)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirectUrl)
      }
    }

    "the user is not logged in" should {
      "return 401 (Unauthorised)" in new MakePaymentDetailsTest {
        private lazy val request = fakeRequestToPOSTWithSession(
          ("amountInPence", "10000"),
          ("taxPeriodMonth", "02"),
          ("taxPeriodYear", "2018"),
          ("periodKey", "#001")
        )
        lazy val result: Future[Result] = target.makePayment()(request)

        override val authResult: Future[Nothing] = Future.failed(MissingBearerToken())

        status(result) shouldBe Status.UNAUTHORIZED
      }
    }

    "the user is not authenticated" should {
      "return 403 (Forbidden)" in new MakePaymentDetailsTest {
        private lazy val request = fakeRequestToPOSTWithSession(
          ("amountInPence", "10000"),
          ("taxPeriodMonth", "02"),
          ("taxPeriodYear", "2018"),
          ("periodKey", "#001")
        )
        lazy val result: Future[Result] = target.makePayment()(request)

        override val authResult: Future[Nothing] = Future.failed(InsufficientEnrolments())

        status(result) shouldBe Status.FORBIDDEN
      }
    }

  }
}

