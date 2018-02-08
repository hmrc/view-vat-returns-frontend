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
import models.errors.{UnexpectedStatusError, UnknownError}
import models.payments.Payment
import models.viewModels.VatReturnViewModel
import models.{User, VatReturn, VatReturnObligation, VatReturnObligations}
import play.api.http.Status
import play.api.test.Helpers._
import services.{EnrolmentsAuthService, ReturnsService, VatApiService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ReturnsControllerSpec extends ControllerBaseSpec {

  val goodEnrolments: Enrolments = Enrolments(
    Set(
      Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VATRegNo", "999999999")), "Active")
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

  val examplePayment: Option[Payment] = Some(Payment(
    LocalDate.parse("2017-01-01"),
    LocalDate.parse("2017-02-01"),
    LocalDate.parse("2017-02-02"),
    1320.00,
    "#001")
  )

  val exampleObligation = VatReturnObligation(
    LocalDate.parse("2017-01-01"),
    LocalDate.parse("2017-02-01"),
    LocalDate.parse("2017-02-02"),
    "F",
    Some(LocalDate.parse("2017-02-02")),
    "#001"
  )

  private trait Test {
    val serviceCall: Boolean = true
    val authResult: Future[Enrolments] = Future.successful(goodEnrolments)
    val vatReturnResult: Future[HttpGetResult[VatReturn]] = Future.successful(Right(exampleVatReturn))
    val paymentResult: Future[Option[Payment]] = Future.successful(examplePayment)
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockVatReturnService: ReturnsService = mock[ReturnsService]
    val mockVatApiService: VatApiService = mock[VatApiService]

    def setup(): Any = {
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(authResult)

      if(serviceCall) {
        (mockVatReturnService.getVatReturnDetails(_: User, _: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returns(vatReturnResult)

        (mockVatReturnService.getReturnObligationsForYear(_: User, _: Int)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returns(Right(VatReturnObligations(Seq(exampleObligation))))

        (mockVatReturnService.getObligationWithMatchingPeriodKey(_: String)(_: HttpGetResult[VatReturnObligations]))
          .expects(*, *)
          .returns(Some(exampleObligation))

        (mockVatReturnService.getPayment(_: User, _: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returns(paymentResult)

        (mockVatApiService.getEntityName(_: User)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(exampleEntityName))
      }
    }

    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)

    def target: ReturnsController = {
      setup()
      new ReturnsController(messages, mockEnrolmentsAuthService, mockVatReturnService, mockVatApiService, mockConfig)
    }
  }

  "Calling the .yourVatReturn action" when {

    "a user is logged in and enrolled to HMRC-MTD-VAT" should {

      "return 200" in new Test {
        private val result = target.vatReturnDetails("#001", 2018)(fakeRequest)
        status(result) shouldBe Status.OK
      }

      "return HTML" in new Test {
        private val result = target.vatReturnDetails("#001", 2018)(fakeRequest)
        contentType(result) shouldBe Some("text/html")
      }

      "return charset of utf-8" in new Test {
        private val result = target.vatReturnDetails("#001", 2018)(fakeRequest)
        charset(result) shouldBe Some("utf-8")
      }
    }

    "a user is not authorised" should {

      "return 403 (Forbidden)" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(InsufficientEnrolments())
        private val result = target.vatReturnDetails("#001", 2018)(fakeRequest)
        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "a user is not authenticated" should {

      "return 401 (Unauthorised)" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(MissingBearerToken())
        private val result = target.vatReturnDetails("#001", 2018)(fakeRequest)
        status(result) shouldBe Status.UNAUTHORIZED
      }
    }

    "the specified VAT return is not found" should {

      "return 404 (Not Found)" in new Test {
        override val vatReturnResult: Future[HttpGetResult[VatReturn]] =
          Future.successful(Left(UnexpectedStatusError(404)))
        private val result = target.vatReturnDetails("#001", 2018)(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
      }
    }

    "a different error is retrieved" should {

      "return 500 (Internal Server Error)" in new Test {
        override val vatReturnResult: Future[HttpGetResult[VatReturn]] =
          Future.successful(Left(UnknownError))
        private val result = target.vatReturnDetails("#001", 2018)(fakeRequest)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "Calling .constructViewModel" should {

    val mockVatReturnService: ReturnsService = mock[ReturnsService]
    val mockVatApiService: VatApiService = mock[VatApiService]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)
    val target = new ReturnsController(messages, mockEnrolmentsAuthService, mockVatReturnService, mockVatApiService, mockConfig)

    "populate a VatReturnViewModel" in {

      val expectedViewModel = VatReturnViewModel(
        entityName = exampleEntityName,
        periodFrom = exampleObligation.start,
        periodTo = exampleObligation.end,
        dueDate = exampleObligation.due,
        dateSubmitted = exampleObligation.received.get,
        boxOne = exampleVatReturn.vatDueSales,
        boxTwo = exampleVatReturn.vatDueAcquisitions,
        outstandingAmount = examplePayment.get.outstandingAmount,
        boxThree = exampleVatReturn.totalVatDue,
        boxFour = exampleVatReturn.vatReclaimedCurrPeriod,
        boxFive = exampleVatReturn.netVatDue,
        boxSix = exampleVatReturn.totalValueSalesExVAT,
        boxSeven = exampleVatReturn.totalValuePurchasesExVAT,
        boxEight = exampleVatReturn.totalValueGoodsSuppliedExVAT,
        boxNine = exampleVatReturn.totalAcquisitionsExVAT,
        showReturnsBreadcrumb = true
      )
      val result: VatReturnViewModel = target.constructViewModel(
        exampleEntityName,
        exampleObligation,
        examplePayment,
        exampleVatReturn,
        isReturnsPageRequest = true
      )
      result shouldBe expectedViewModel
    }
  }
}
