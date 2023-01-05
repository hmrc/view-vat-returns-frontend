/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationBaseSpec
import models.Obligation.Status
import models.errors.{ApiSingleError, BadRequestError, MultipleErrors}
import models.{VatReturnObligation, VatReturnObligations}
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import stubs.VatObligationsStub
import uk.gov.hmrc.http.HeaderCarrier

class VatObligationsConnectorISpec extends IntegrationBaseSpec {

  private trait Test {
    def setupStubs(): StubMapping

    val connector: VatObligationsConnector = app.injector.instanceOf[VatObligationsConnector]
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val obligationsStub = new VatObligationsStub(true)
  }

  "Calling getVatReturnObligations with a status of 'A'" should {

    "return all VAT return obligations for a given period" in new Test {
      override def setupStubs(): StubMapping = obligationsStub.stubAllObligations

      val expected = Right(VatReturnObligations(
        Seq(
          VatReturnObligation(
            periodFrom = LocalDate.parse("2018-01-01"),
            periodTo = LocalDate.parse("2018-03-31"),
            due = LocalDate.parse("2018-05-07"),
            status = "F",
            received = Some(LocalDate.parse("2018-04-15")),
            periodKey = "#001"
          ),
          VatReturnObligation(
            periodFrom = LocalDate.parse("2018-01-01"),
            periodTo = LocalDate.parse("2018-03-31"),
            due = LocalDate.parse("2018-05-07"),
            status = "O",
            received = None,
            periodKey = "#004"
          )
        )
      ))

      setupStubs()
      private val result = await(connector.getVatReturnObligations("123456789",
        Some(LocalDate.parse("2017-01-01")),
        Some(LocalDate.parse("2017-12-31")),
        Status.All
      ))

      result shouldBe expected
    }
  }

  "Calling getVatReturnObligations with a status of 'O'" should {

    "return all outstanding obligations for a given period" in new Test {
      override def setupStubs(): StubMapping = obligationsStub.stubOutstandingObligations

      val expected = Right(VatReturnObligations(
        Seq(
          VatReturnObligation(
            periodFrom = LocalDate.parse("2018-01-01"),
            periodTo = LocalDate.parse("2018-03-31"),
            due = LocalDate.parse("2018-05-07"),
            status = "O",
            received = None,
            periodKey = "#004"
          )
        )
      ))

      setupStubs()
      private val result = await(connector.getVatReturnObligations("123456789",
        Some(LocalDate.parse("2017-01-01")),
        Some(LocalDate.parse("2017-12-31")),
        Status.Outstanding
      ))

      result shouldBe expected
    }
  }

  "Calling getVatReturnObligations with a status of 'F'" should {

    "return all fulfilled obligations for a given period" in new Test {
      override def setupStubs(): StubMapping = obligationsStub.stubFulfilledObligations

      val expected = Right(VatReturnObligations(
        Seq(
          VatReturnObligation(
            periodFrom = LocalDate.parse("2018-01-01"),
            periodTo = LocalDate.parse("2018-03-31"),
            due = LocalDate.parse("2018-05-07"),
            status = "F",
            received = Some(LocalDate.parse("2018-04-15")),
            periodKey = "#001"
          )
        )
      ))

      setupStubs()
      private val result = await(connector.getVatReturnObligations("123456789",
        Some(LocalDate.parse("2017-01-01")),
        Some(LocalDate.parse("2017-12-31")),
        Status.Fulfilled
      ))

      result shouldBe expected
    }
  }

  "Calling getVatReturnObligations with an invalid VRN" should {

    "return a BadRequestError" in new Test {
      override def setupStubs(): StubMapping = obligationsStub.stubInvalidVrnForObligations

      val expected = Left(BadRequestError(
        code = "VRN_INVALID",
        errorResponse = ""
      ))

      setupStubs()
      private val result = await(connector.getVatReturnObligations("123456789",
        Some(LocalDate.parse("2017-01-01")),
        Some(LocalDate.parse("2017-12-31")),
        Status.Outstanding
      ))

      result shouldBe expected
    }
  }

  "Calling getVatReturnObligations with an invalid 'from' date" should {

    "return a BadRequestError" in new Test {
      override def setupStubs(): StubMapping = obligationsStub.stubInvalidFromDate

      val expected = Left(BadRequestError(
        code = "INVALID_DATE_FROM",
        errorResponse = ""
      ))

      setupStubs()
      private val result = await(connector.getVatReturnObligations("123456789",
        Some(LocalDate.parse("2017-01-01")),
        Some(LocalDate.parse("2017-12-31")),
        Status.Fulfilled
      ))

      result shouldBe expected
    }
  }

  "Calling getVatReturnObligations with an invalid 'to' date" should {

    "return a BadRequestError" in new Test {
      override def setupStubs(): StubMapping = obligationsStub.stubInvalidToDate

      val expected = Left(BadRequestError(
        code = "INVALID_DATE_TO",
        errorResponse = ""
      ))

      setupStubs()
      private val result = await(connector.getVatReturnObligations("123456789",
        Some(LocalDate.parse("2017-01-01")),
        Some(LocalDate.parse("2017-12-31")),
        Status.Fulfilled
      ))

      result shouldEqual expected
    }
  }

  "Calling getVatReturnObligations with no to and from dates" should {

    "return all outstanding obligations" in new Test {
      override def setupStubs(): StubMapping = obligationsStub.stubOutstandingObligations

      val expected = Right(VatReturnObligations(
        Seq(
          VatReturnObligation(
            periodFrom = LocalDate.parse("2018-01-01"),
            periodTo = LocalDate.parse("2018-03-31"),
            due = LocalDate.parse("2018-05-07"),
            status = "O",
            received = None,
            periodKey = "#004"
          )
        )
      ))

      setupStubs()
      private val result = await(connector.getVatReturnObligations("123456789",
        None,
        None,
        Status.Outstanding
      ))

      result shouldBe expected
    }

  }

  "Calling getVatReturnObligations with an invalid date range" should {

    "return a BadRequestError" in new Test {
      override def setupStubs(): StubMapping = obligationsStub.stubInvalidDateRange

      val expected = Left(BadRequestError(
        code = "INVALID_DATE_RANGE",
        errorResponse = ""
      ))

      setupStubs()
      private val result = await(connector.getVatReturnObligations("123456789",
        Some(LocalDate.parse("2017-01-01")),
        Some(LocalDate.parse("2017-12-31")),
        Status.Fulfilled
      ))

      result shouldEqual expected
    }
  }

  "Calling getVatReturnObligations with an invalid obligation status" should {

    "return an BadRequestError" in new Test {
      override def setupStubs(): StubMapping = obligationsStub.stubInvalidStatus

      val expected = Left(BadRequestError(
        code = "INVALID_STATUS",
        errorResponse = ""
      ))

      setupStubs()
      private val result = await(connector.getVatReturnObligations("123456789",
        Some(LocalDate.parse("2017-01-01")),
        Some(LocalDate.parse("2017-12-31")),
        Status.Fulfilled
      ))

      result shouldEqual expected
    }
  }

  "Calling getVatReturnObligations with multiple errors" should {

    "return a MultipleErrors" in new Test {
      override def setupStubs(): StubMapping = obligationsStub.stubMultipleErrors

      val errors = Seq(ApiSingleError("ERROR_1", "MESSAGE_1"), ApiSingleError("ERROR_2", "MESSAGE_2"))
      val expected = Left(MultipleErrors("BAD_REQUEST", Json.toJson(errors).toString()))
      setupStubs()
      private val result = await(connector.getVatReturnObligations("123456789",
        Some(LocalDate.parse("2017-01-01")),
        Some(LocalDate.parse("2017-12-31")),
        Status.Fulfilled
      ))

      result shouldBe expected
    }
  }
}