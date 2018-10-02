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

import audit.AuditingService
import audit.models.AuditModel
import models._
import models.User
import models.customer.CustomerDetail
import models.errors.{DirectDebitStatusError, NotFoundError, VatReturnError}
import models.payments.Payment
import models.viewModels.VatReturnViewModel
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import services.{DateService, EnrolmentsAuthService, ReturnsService, SubscriptionService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ReturnsControllerSpec extends ControllerBaseSpec {

  val goodEnrolments: Enrolments = Enrolments(
    Set(
      Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "999999999")), "Active")
    )
  )
  val exampleVatReturn: VatReturn = VatReturn(
    "#001",
    1297,
    5755,
    7052,
    5732,
    1320,
    77656,
    765765,
    55454,
    545645
  )

  val exampleEntityName: Option[String] = Some("Cheapo Clothing")
  val exampleCustomerDetail: Option[CustomerDetail] = Some(CustomerDetail("Cheapo Clothing", hasFlatRateScheme = true, isPartialMigration = false))

  val examplePayment: Payment = Payment(
    "VAT",
    LocalDate.parse("2017-01-01"),
    LocalDate.parse("2017-02-01"),
    LocalDate.parse("2017-02-02"),
    1320.00,
    0,
    "#001"
  )

  val exampleObligation = VatReturnObligation(
    LocalDate.parse("2017-01-01"),
    LocalDate.parse("2017-02-01"),
    LocalDate.parse("2017-02-02"),
    "F",
    Some(LocalDate.parse("2017-02-02")),
    "#001"
  )

  val exampleVatReturnDetails =
    VatReturnDetails(exampleVatReturn, moneyOwed = true, isRepayment = false, Some(examplePayment))

  val mockAuditService: AuditingService = mock[AuditingService]
  val mockDateService: DateService = mock[DateService]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockVatReturnService: ReturnsService = mock[ReturnsService]
  val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)
  val mockVatApiService: SubscriptionService = mock[SubscriptionService]
  val mockSubscriptionService: SubscriptionService = mock[SubscriptionService]

  private trait Test {
    val serviceCall: Boolean = true
    val successReturn: Boolean = true
    val directDebitStatus: ServiceResponse[Boolean] = Right(false)
    val authResult: Future[Enrolments] = Future.successful(goodEnrolments)
    val vatReturnResult: Future[ServiceResponse[VatReturn]] = Future.successful(Right(exampleVatReturn))
    val paymentResult: Future[Option[Payment]] = Future.successful(Some(examplePayment))

    def setup(): Any = {
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(authResult)

      if (serviceCall) {
        (mockVatReturnService.getVatReturn(_: User, _: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returns(vatReturnResult)

        (mockVatReturnService.getObligationWithMatchingPeriodKey(_: User, _: Int, _: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *, *)
          .returns(Some(exampleObligation))

        (mockVatReturnService.getPayment(_: User, _: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returns(paymentResult)

        (mockVatReturnService.getDirectDebitStatus(_: String)(_: HeaderCarrier, _:ExecutionContext))
          .expects(*, *, *)
          .returns(directDebitStatus)

        (mockSubscriptionService.getUserDetails(_: User)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(exampleCustomerDetail))

        if (successReturn) {
          (mockVatReturnService.constructReturnDetailsModel(_: VatReturn, _: Option[Payment]))
            .expects(*, *)
            .returns(exampleVatReturnDetails)
        }

        (mockAuditService.audit(_: AuditModel, _: String)(_: HeaderCarrier, _: ExecutionContext))
          .stubs(*, *, *, *)
          .returns({})

        (mockDateService.now: () => LocalDate).stubs().returns(LocalDate.parse("2018-05-01"))
      }
    }

    def target: ReturnsController = {
      setup()
      new ReturnsController(
        messages,
        mockEnrolmentsAuthService,
        mockVatReturnService,
        mockSubscriptionService,
        mockDateService,
        mockConfig,
        mockAuditService
      )
    }
  }

  "Calling the .vatReturn action" when {

    "a user is logged in and enrolled to HMRC-MTD-VAT" should {

      "return 200" in new Test {
        private val result = target.vatReturn(2018, "#001")(fakeRequest)
        status(result) shouldBe Status.OK
      }

      "return HTML" in new Test {
        private val result = target.vatReturn(2018, "#001")(fakeRequest)
        contentType(result) shouldBe Some("text/html")
      }

      "return charset of utf-8" in new Test {
        private val result = target.vatReturn(2018, "#001")(fakeRequest)
        charset(result) shouldBe Some("utf-8")
      }
    }

    "a user is not authorised" should {

      "return 403 (Forbidden)" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(InsufficientEnrolments())
        private val result = target.vatReturn(2018, "#001")(fakeRequest)
        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "a user is not authenticated" should {

      "return 401 (Unauthorised)" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(MissingBearerToken())
        private val result = target.vatReturn(2018, "#001")(fakeRequest)
        status(result) shouldBe Status.UNAUTHORIZED
      }
    }

    "the specified VAT return is not found" should {

      "return 404 (Not Found)" in new Test {
        override val successReturn = false
        override val vatReturnResult: Future[ServiceResponse[Nothing]] = Left(NotFoundError)
        private val result = target.vatReturn(2018, "#001")(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
      }
    }

    "a different error is retrieved" should {

      "return 500 (Internal Server Error)" in new Test {
        override val successReturn = false
        override val vatReturnResult: Future[ServiceResponse[Nothing]] = Left(VatReturnError)
        private val result = target.vatReturn(2018, "#001")(fakeRequest)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "Calling the .vatReturnViaPayments action" when {

    "a user is logged in and enrolled to HMRC-MTD-VAT" should {

      "return 200" in new Test {
        private val result = target.vatReturnViaPayments("#001")(fakeRequest)
        status(result) shouldBe Status.OK
      }

      "return HTML" in new Test {
        private val result = target.vatReturnViaPayments("#001")(fakeRequest)
        contentType(result) shouldBe Some("text/html")
      }

      "return charset of utf-8" in new Test {
        private val result = target.vatReturnViaPayments("#001")(fakeRequest)
        charset(result) shouldBe Some("utf-8")
      }
    }

    "a user is not authorised" should {

      "return 403 (Forbidden)" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(InsufficientEnrolments())
        private val result = target.vatReturnViaPayments("#001")(fakeRequest)
        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "a user is not authenticated" should {

      "return 401 (Unauthorised)" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(MissingBearerToken())
        private val result = target.vatReturnViaPayments("#001")(fakeRequest)
        status(result) shouldBe Status.UNAUTHORIZED
      }
    }

    "the specified VAT return is not found" should {

      "return 404 (Not Found)" in new Test {
        override val successReturn = false
        override val vatReturnResult: Future[ServiceResponse[Nothing]] = Left(NotFoundError)
        private val result = target.vatReturnViaPayments("#001")(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
      }
    }

    "a different error is retrieved" should {

      "return 500 (Internal Server Error)" in new Test {
        override val successReturn = false
        override val vatReturnResult: Future[ServiceResponse[Nothing]] = Left(VatReturnError)
        private val result = target.vatReturnViaPayments("#001")(fakeRequest)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "Calling .constructViewModel" should {

    val target = new ReturnsController(
      messages,
      mockEnrolmentsAuthService,
      mockVatReturnService,
      mockVatApiService,
      mockDateService,
      mockConfig,
      mockAuditService
    )

    "populate a VatReturnViewModel" in {

      val expectedViewModel = VatReturnViewModel(
        entityName = Some("Cheapo Clothing"),
        periodFrom = exampleObligation.start,
        periodTo = exampleObligation.end,
        dueDate = exampleObligation.due,
        outstandingAmount = examplePayment.outstandingAmount,
        dateSubmitted = exampleObligation.received.get,
        vatReturnDetails = exampleVatReturnDetails,
        showReturnsBreadcrumb = true,
        currentYear = 2018,
        hasFlatRateScheme = true,
        hasDirectDebit = false,
        isHybridUser = false
      )

      (mockDateService.now: () => LocalDate).stubs().returns(LocalDate.parse("2018-05-01"))

      val result: VatReturnViewModel = target.constructViewModel(
        exampleCustomerDetail,
        exampleObligation,
        exampleVatReturnDetails,
        isReturnsPageRequest = true,
        directDebitStatus = false
      )
      result shouldBe expectedViewModel
    }
  }

  "Calling .renderResult" when {

    val user = models.User("123456789", hasNonMtdVat = true)

    val target = new ReturnsController(
      messages,
      mockEnrolmentsAuthService,
      mockVatReturnService,
      mockVatApiService,
      mockDateService,
      mockConfig,
      mockAuditService
    )

    "there is a VAT return, obligation and payment" when {

      def successSetup(): Any = {
        (mockVatReturnService.constructReturnDetailsModel(_: VatReturn, _: Option[Payment]))
          .expects(*, *)
          .returns(exampleVatReturnDetails)

        (mockDateService.now: () => LocalDate).stubs().returns(LocalDate.parse("2018-05-01"))

        (mockAuditService.audit(_: AuditModel, _: String)(_: HeaderCarrier, _: ExecutionContext))
          .stubs(*, *, *, *)
          .returns({})
      }
      val data = ReturnsControllerData(Right(exampleVatReturn), None, Some(examplePayment), Some(exampleObligation), Right(false))
      def result: Result = target.renderResult(data, isReturnsPageRequest = true)(fakeRequest, user)

      "return an OK status" in {
        successSetup()
        result.header.status shouldBe Status.OK
      }
    }

    "the VAT return is not found" should {

      "return a Not Found status" in {
        val data = ReturnsControllerData(Left(NotFoundError), None, None, None, Left(DirectDebitStatusError))
        val result = target.renderResult(data, isReturnsPageRequest = true)(fakeRequest, user)
        result.header.status shouldBe Status.NOT_FOUND
      }
    }

    "there is a VAT return but no obligation" should {

      "return an Internal Server Error status" in {
        val data = ReturnsControllerData(Right(exampleVatReturn), None, None, None, Right(false))
        val result = target.renderResult(data, isReturnsPageRequest = true)(fakeRequest, user)
        result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "there is any other combination" should {

      "return an Internal Server Error status" in {
        val data = ReturnsControllerData(Left(VatReturnError), None, None, None, Left(DirectDebitStatusError))
        val result = target.renderResult(data, isReturnsPageRequest = true)(fakeRequest, user)
        result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "Calling .getDirectDebitStatus" when {

    "the service response is successful" should {

      "return the boolean in the service response" in new Test {
        override def setup(): Any = ()
        val serviceResponse = Right(true)
        val result: Boolean = target.getDirectDebitStatus(serviceResponse)
        result shouldBe true
      }
    }

    "the service response fails" should {

      "return false" in new Test {
        override def setup(): Any = ()
        val serviceResponse = Left(DirectDebitStatusError)
        val result: Boolean = target.getDirectDebitStatus(serviceResponse)
        result shouldBe false
      }
    }
  }
}