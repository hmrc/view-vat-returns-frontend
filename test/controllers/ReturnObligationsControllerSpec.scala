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

import models.errors.{BadRequestError, HttpError}
import models.viewModels.{ReturnObligationsViewModel, VatReturnsViewModel}
import models.{User, VatReturnObligation, VatReturnObligations}
import play.api.http.Status
import play.api.test.Helpers._
import services.{EnrolmentsAuthService, ReturnsService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ReturnObligationsControllerSpec extends ControllerBaseSpec {

  private trait Test {
    val goodEnrolments: Enrolments = Enrolments(
      Set(
        Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VATRegNo", "999999999")), "Active")
      )
    )

    val exampleObligations: VatReturnObligations = VatReturnObligations(
      Seq(
        VatReturnObligation(
          LocalDate.parse("2017-01-01"),
          LocalDate.parse("2017-12-31"),
          LocalDate.parse("2018-01-31"),
          "O",
          None,
          "#001"
        )
      )
    )

    val serviceCall: Boolean = true
    val authResult: Future[_]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockVatReturnService: ReturnsService = mock[ReturnsService]

    def setup(): Any = {
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(authResult)

      if (serviceCall) {
        (mockVatReturnService.getReturnObligationsForYear(_: User, _: Int, _: VatReturnObligation.Status.Value)
        (_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *, *)
          .returns(Future.successful(Right(exampleObligations)))
      }
    }

    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)

    def target: ReturnObligationsController = {
      setup()
      new ReturnObligationsController(messages, mockEnrolmentsAuthService, mockVatReturnService, mockConfig)
    }
  }

  private trait HandleReturnObligationsTest {
    val vatServiceResult: Future[Either[HttpError, VatReturnObligations]]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockVatReturnService: ReturnsService = mock[ReturnsService]
    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)
    val testUser: User = User("999999999")
    implicit val hc: HeaderCarrier = HeaderCarrier()

    def setup(): Any = {
      (mockVatReturnService.getReturnObligationsForYear(_: User, _: Int, _: VatReturnObligation.Status.Value)
      (_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *)
        .returns(vatServiceResult)
    }

    def target: ReturnObligationsController = {
      setup()
      new ReturnObligationsController(messages, mockEnrolmentsAuthService, mockVatReturnService, mockConfig)
    }
  }

  "Calling the .completedReturns action" when {

    "A user is logged in and enrolled to HMRC-MTD-VAT" should {

      "return 200" in new Test {
        override val authResult: Future[Enrolments] = Future.successful(goodEnrolments)
        private val result = target.completedReturns(LocalDate.now().getYear -1)(fakeRequest)
        status(result) shouldBe Status.OK
      }

      "return HTML" in new Test {
        override val authResult: Future[Enrolments] = Future.successful(goodEnrolments)
        private val result = target.completedReturns(LocalDate.now().getYear -1)(fakeRequest)
        contentType(result) shouldBe Some("text/html")
      }

      "return charset of utf-8" in new Test {
        override val authResult: Future[Enrolments] = Future.successful(goodEnrolments)
        private val result = target.completedReturns(LocalDate.now().getYear -1)(fakeRequest)
        charset(result) shouldBe Some("utf-8")
      }
    }

    "A user is not authorised" should {

      "return 403 (Forbidden)" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(InsufficientEnrolments())
        private val result = target.completedReturns(LocalDate.now().getYear -1)(fakeRequest)
        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "A user is not authenticated" should {

      "return 401 (Unauthorised)" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(MissingBearerToken())
        private val result = target.completedReturns(LocalDate.now().getYear -1)(fakeRequest)
        status(result) shouldBe Status.UNAUTHORIZED
      }
    }

    "A user enters an invalid search year for their completed returns" should {

      "return 404 (Not Found)" in new Test {
        override val serviceCall = false
        override val authResult: Future[_] = Future.successful(goodEnrolments)
        private val result = target.completedReturns(LocalDate.now().getYear +3)(fakeRequest)
        status(result) shouldBe Status.NOT_FOUND
      }

    }
  }

  "Calling the .returnDeadlines action" when {

    "A user is logged in and enrolled to HMRC-MTD-VAT" should {

      "return 200" in new Test {
        override val serviceCall = false
        override val authResult: Future[Enrolments] = Future.successful(goodEnrolments)
        private val result = target.returnDeadlines()(fakeRequest)
        status(result) shouldBe Status.OK
      }

      "return HTML" in new Test {
        override val serviceCall = false
        override val authResult: Future[Enrolments] = Future.successful(goodEnrolments)
        private val result = target.returnDeadlines()(fakeRequest)
        contentType(result) shouldBe Some("text/html")
      }

      "return charset of utf-8" in new Test {
        override val serviceCall = false
        override val authResult: Future[Enrolments] = Future.successful(goodEnrolments)
        private val result = target.returnDeadlines()(fakeRequest)
        charset(result) shouldBe Some("utf-8")
      }
    }

    "A user is not authorised" should {

      "return 403 (Forbidden)" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(InsufficientEnrolments())
        private val result = target.returnDeadlines()(fakeRequest)
        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "A user is not authenticated" should {

      "return 401 (Unauthorised)" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(MissingBearerToken())
        private val result = target.returnDeadlines()(fakeRequest)
        status(result) shouldBe Status.UNAUTHORIZED
      }
    }
  }

  "Calling the .handleReturnObligations function" when {

    "the vatReturnsService retrieves a valid list of VatReturnObligations" should {

      "return a VatReturnsViewModel with valid obligations" in new HandleReturnObligationsTest {
        override val vatServiceResult: Future[Right[HttpError, VatReturnObligations]] = Future.successful {
          Right(
            VatReturnObligations(Seq(VatReturnObligation(
              LocalDate.parse("2017-01-01"),
              LocalDate.parse("2017-01-01"),
              LocalDate.parse("2017-01-01"),
              "O",
              None,
              "#001"
            )))
          )
        }

        val currentYear: Int = LocalDate.now().getYear
        val expectedResult = VatReturnsViewModel(
          Seq(currentYear, currentYear - 1, currentYear - 2, currentYear - 3),
          2017,
          Seq(
            ReturnObligationsViewModel(
              LocalDate.parse("2017-01-01"),
              LocalDate.parse("2017-01-01"),
              "#001"
            )
          )
        )

        private val result = await(target.getReturnObligations(testUser, 2017, VatReturnObligation.Status.All))
        result shouldBe expectedResult
      }
    }

    "the vatReturnsService retrieves an error" should {

      "return a VatReturnsViewModel with empty obligations" in new HandleReturnObligationsTest {
        override val vatServiceResult: Future[Either[HttpError, VatReturnObligations]] = Future.successful {
          Left(BadRequestError("", ""))
        }

        val currentYear: Int = LocalDate.now().getYear
        val expectedResult = VatReturnsViewModel(
          Seq(currentYear, currentYear - 1, currentYear - 2, currentYear - 3),
          2017,
          Seq()
        )

        private val result = await(target.getReturnObligations(testUser, 2017, VatReturnObligation.Status.All))
        result shouldBe expectedResult
      }
    }
  }

  "Calling .validateSearchYear" when {

    "the year is on the upper search boundary" should {

      "return true" in new Test {
        override val authResult: Future[_] = Future.successful("")

        override def setup(): Any = "" // Prevent the unused mocks causing trouble

        val result: Boolean = target.validateSearchYear(2018, 2018)

        result shouldBe true
      }

    }

    "the year is above the upper search boundary" should {

      "return false" in new Test {
        override val authResult: Future[_] = Future.successful("")

        override def setup(): Any = "" // Prevent the unused mocks causing trouble

        val result: Boolean = target.validateSearchYear(2019, 2018)

        result shouldBe false
      }

    }

    "the year is on the lower boundary" should {

      "return true" in new Test {
        override val authResult: Future[_] = Future.successful("")

        override def setup(): Any = "" // Prevent the unused mocks causing trouble

        val result: Boolean = target.validateSearchYear(2015, 2018)

        result shouldBe true
      }

    }

    "the year is below the lower boundary" should {

      "return false" in new Test {
        override val authResult: Future[_] = Future.successful("")

        override def setup(): Any = "" // Prevent the unused mocks causing trouble

        val result: Boolean = target.validateSearchYear(2014, 2018)

        result shouldBe false
      }

    }

    "the year is between the upper and lower boundaries" should {

      "return true" in new Test {
        override val authResult: Future[_] = Future.successful("")

        override def setup(): Any = "" // Prevent the unused mocks causing trouble

        val result: Boolean = target.validateSearchYear(2017, 2018)

        result shouldBe true
      }

    }

  }

}
