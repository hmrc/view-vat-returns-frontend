/*
 * Copyright 2017 HM Revenue & Customs
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

import models.{User, VatReturn}
import play.api.http.Status
import play.api.test.Helpers._
import services.{EnrolmentsAuthService, VatReturnService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class VatReturnControllerSpec extends ControllerBaseSpec {

  private trait Test {
    val exampleVatReturn: VatReturn = VatReturn(
      "ABC Clothing",
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      LocalDate.parse("2017-04-08"),
      99999,
      77777,
      4444,
      5555,
      999999,
      9444444,
      9999,
      7777,
      999.54
    )
    val serviceCall: Boolean = true
    val authResult: Future[_]
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockVatReturnService: VatReturnService = mock[VatReturnService]

    def setup(): Any = {
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(authResult)

      if(serviceCall) {
        (mockVatReturnService.getVatReturn(_: User))
          .expects(*)
          .returns(Future.successful(exampleVatReturn))
      }
    }

    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)

    def target: VatReturnController = {
      setup()
      new VatReturnController(messages, mockEnrolmentsAuthService, mockVatReturnService, mockConfig)
    }
  }

  "Calling the .yourVatReturn action" when {

    "A user is logged in and enrolled to HMRC-MTD-VAT" should {

      val goodEnrolments: Enrolments = Enrolments(
        Set(
          Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("", "999999999")), "Active")
        )
      )

      "return 200" in new Test {
        override val authResult: Future[Enrolments] = Future.successful(goodEnrolments)
        private val result = target.yourVatReturn()(fakeRequest)
        status(result) shouldBe Status.OK
      }

      "return HTML" in new Test {
        override val authResult: Future[Enrolments] = Future.successful(goodEnrolments)
        private val result = target.yourVatReturn()(fakeRequest)
        contentType(result) shouldBe Some("text/html")
      }

      "return charset of utf-8" in new Test {
        override val authResult: Future[Enrolments] = Future.successful(goodEnrolments)
        private val result = target.yourVatReturn()(fakeRequest)
        charset(result) shouldBe Some("utf-8")
      }
    }

    "A user is logged in but not enrolled to HMRC-MTD-VAT" should {

      "return 303" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(InsufficientEnrolments())
        private val result = target.yourVatReturn()(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to the unauthorised page" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(InsufficientEnrolments())
        private val result = target.yourVatReturn()(fakeRequest)
        redirectLocation(result) shouldBe Some(routes.ErrorsController.unauthorised().url)
      }
    }

    "A user is not logged in" should {

      "return 303" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(MissingBearerToken())
        private val result = target.yourVatReturn()(fakeRequest)
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to the session timeout page" in new Test {
        override val serviceCall = false
        override val authResult: Future[Nothing] = Future.failed(MissingBearerToken())
        private val result = target.yourVatReturn()(fakeRequest)
        redirectLocation(result) shouldBe Some(routes.ErrorsController.sessionTimeout().url)
      }
    }
  }
}
