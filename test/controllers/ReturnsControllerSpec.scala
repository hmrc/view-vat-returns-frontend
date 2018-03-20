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

import connectors.httpParsers.ResponseHttpParsers.HttpGetResult
import models._
import models.errors.{ServerSideError, UnexpectedStatusError, UnknownError}
import models.payments.Payment
import models.viewModels.VatReturnViewModel
import play.api.http.Status
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

  val examplePayment: Payment = Payment(
    LocalDate.parse("2017-01-01"),
    LocalDate.parse("2017-02-01"),
    LocalDate.parse("2017-02-02"),
    1320.00,
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

  val exampleVatReturnDetails = VatReturnDetails(exampleVatReturn, moneyOwed = true, isRepayment = false, examplePayment)

  private trait Test {
    val serviceCall: Boolean = true
    val successReturn: Boolean = true
    val authResult: Future[Enrolments] = Future.successful(goodEnrolments)
    val vatReturnResult: Future[HttpGetResult[VatReturn]] = Future.successful(Right(exampleVatReturn))
    val paymentResult: Future[Option[Payment]] = Future.successful(Some(examplePayment))
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockVatReturnService: ReturnsService = mock[ReturnsService]
    val mockSubscriptionService: SubscriptionService = mock[SubscriptionService]
    val mockDateService: DateService = mock[DateService]

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

        (mockSubscriptionService.getEntityName(_: User)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(exampleEntityName))

        if(successReturn) {
          (mockVatReturnService.constructReturnDetailsModel(_: VatReturn, _: Payment))
            .expects(*, *)
            .returns(exampleVatReturnDetails)
        }

        (mockDateService.now: () => LocalDate).stubs().returns(LocalDate.parse("2018-05-01"))
      }
    }

    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)

    def target: ReturnsController = {
      setup()
      new ReturnsController(messages, mockEnrolmentsAuthService, mockVatReturnService, mockSubscriptionService, mockDateService, mockConfig)
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
        override val vatReturnResult: Future[HttpGetResult[VatReturn]] =
          Future.successful(Left(UnexpectedStatusError(404, "test")))
        private val result = target.vatReturn(2018, "#001")(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
      }
    }

    "a different error is retrieved" should {

      "return 500 (Internal Server Error)" in new Test {
        override val successReturn = false
        override val vatReturnResult: Future[HttpGetResult[VatReturn]] =
          Future.successful(Left(UnknownError))
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
        override val vatReturnResult: Future[HttpGetResult[VatReturn]] =
          Future.successful(Left(UnexpectedStatusError(404, "test")))
        private val result = target.vatReturnViaPayments("#001")(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
      }
    }

    "a different error is retrieved" should {

      "return 500 (Internal Server Error)" in new Test {
        override val successReturn = false
        override val vatReturnResult: Future[HttpGetResult[VatReturn]] =
          Future.successful(Left(UnknownError))
        private val result = target.vatReturnViaPayments("#001")(fakeRequest)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "Calling .constructViewModel" should {

    val mockVatReturnService: ReturnsService = mock[ReturnsService]
    val mockVatApiService: SubscriptionService = mock[SubscriptionService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockDateService: DateService = mock[DateService]
    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)
    val target = new ReturnsController(messages, mockEnrolmentsAuthService, mockVatReturnService, mockVatApiService, mockDateService, mockConfig)

    "populate a VatReturnViewModel" in {

      val expectedViewModel = VatReturnViewModel(
        entityName = exampleEntityName,
        periodFrom = exampleObligation.start,
        periodTo = exampleObligation.end,
        dueDate = exampleObligation.due,
        outstandingAmount = examplePayment.outstandingAmount,
        dateSubmitted = exampleObligation.received.get,
        boxOne = exampleVatReturn.vatDueSales,
        boxTwo = exampleVatReturn.vatDueAcquisitions,
        boxThree = exampleVatReturn.totalVatDue,
        boxFour = exampleVatReturn.vatReclaimedCurrentPeriod,
        boxFive = exampleVatReturn.netVatDue,
        boxSix = exampleVatReturn.totalSalesExcludingVAT,
        boxSeven = exampleVatReturn.totalPurchasesExcludingVAT,
        boxEight = exampleVatReturn.totalGoodsSuppliedExcludingVAT,
        boxNine = exampleVatReturn.totalAcquisitionsExcludingVAT,
        moneyOwed = true,
        isRepayment = false,
        showReturnsBreadcrumb = true,
        currentYear = 2018
      )

      (mockDateService.now: () => LocalDate).stubs().returns(LocalDate.parse("2018-05-01"))

      val result: VatReturnViewModel = target.constructViewModel(
        exampleEntityName,
        exampleObligation,
        exampleVatReturnDetails,
        isReturnsPageRequest = true
      )
      result shouldBe expectedViewModel
    }
  }

  "Calling .renderResult" when {

    val mockVatReturnService: ReturnsService = mock[ReturnsService]
    val mockVatApiService: SubscriptionService = mock[SubscriptionService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockDateService: DateService = mock[DateService]
    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)
    val target = new ReturnsController(messages, mockEnrolmentsAuthService, mockVatReturnService, mockVatApiService, mockDateService, mockConfig)

    "it returns Right(vatReturn), Some(ob) and Some(pay)" should {

      "return an OK status" in {

        (mockVatReturnService.constructReturnDetailsModel(_: VatReturn, _: Payment))
          .expects(*, *)
          .returns(exampleVatReturnDetails)

        (mockDateService.now: () => LocalDate).stubs().returns(LocalDate.parse("2018-05-01"))

        val data = ReturnsControllerData(Right(exampleVatReturn), None, Some(examplePayment), Some(exampleObligation))
        val result = target.renderResult(data, isReturnsPageRequest = true)(fakeRequest)
        result.header.status shouldBe Status.OK
      }
    }

    "it returns Right(_), None and _" should {

      "return a Not Found status" in {
        val data = ReturnsControllerData(Right(exampleVatReturn), None, Some(examplePayment), None)
        val result = target.renderResult(data, isReturnsPageRequest = true)(fakeRequest)
        result.header.status shouldBe Status.NOT_FOUND
      }
    }

    "it returns Right(_), _ and None" should {

      "return a Not Found status" in {
        val data = ReturnsControllerData(Right(exampleVatReturn), None, None, Some(exampleObligation))
        val result = target.renderResult(data, isReturnsPageRequest = true)(fakeRequest)
        result.header.status shouldBe Status.NOT_FOUND
      }
    }

    "it returns Left(UnexpectedStatusError(404)), _ and _" should {

      "return a Not Found status" in {
        val data = ReturnsControllerData(Left(UnexpectedStatusError(404, "test")), None, None, None)
        val result = target.renderResult(data, isReturnsPageRequest = true)(fakeRequest)
        result.header.status shouldBe Status.NOT_FOUND
      }
    }

    "it returns anything else" should {

      "return an Internal Server Error status" in {
        val data = ReturnsControllerData(Left(ServerSideError(500, "test")), None, None, None)
        val result = target.renderResult(data, isReturnsPageRequest = true)(fakeRequest)
        result.header.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
