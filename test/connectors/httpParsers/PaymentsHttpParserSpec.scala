/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors.httpParsers

import java.time.LocalDate

import connectors.httpParsers.PaymentsHttpParser.PaymentsReads
import controllers.ControllerBaseSpec
import models.errors._
import models.payments.{Payment, Payments}
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HttpResponse

class PaymentsHttpParserSpec extends ControllerBaseSpec {

  "PaymentsReads" when {

    "the http response status is 200 OK and there are valid charge types" should {

      val httpResponse = HttpResponse.apply(Status.OK,
        Json.obj(
          "financialTransactions" -> Json.arr(
            Json.obj(
              "mainType" -> "VAT Return Charge",
              "chargeType" -> "VAT Return Debit Charge",
              "taxPeriodFrom" -> "2016-12-01",
              "taxPeriodTo" -> "2017-01-01",
              "items" -> Json.arr(
                Json.obj("dueDate" -> "2017-10-25")
              ),
              "outstandingAmount" -> 1000,
              "periodKey" -> "#003"
            ),
            Json.obj(
              "mainType" -> "VAT Return Charge",
              "chargeType" -> "VAT Return Credit Charge",
              "taxPeriodFrom" -> "2017-12-01",
              "taxPeriodTo" -> "2018-01-01",
              "items" -> Json.arr(
                Json.obj("dueDate" -> "2018-10-25")
              ),
              "outstandingAmount" -> -1000,
              "periodKey" -> "#004"
            ),
            Json.obj(
              "mainType" -> "VAT AA Return Charge",
              "chargeType" -> "VAT AA Return Debit Charge",
              "taxPeriodFrom" -> "2018-12-01",
              "taxPeriodTo" -> "2019-01-01",
              "items" -> Json.arr(
                Json.obj("dueDate" -> "2019-10-25")
              ),
              "outstandingAmount" -> 1000,
              "periodKey" -> "#005"
            ),
            Json.obj(
              "mainType" -> "VAT AA Return Charge",
              "chargeType" -> "VAT AA Return Credit Charge",
              "taxPeriodFrom" -> "2019-12-01",
              "taxPeriodTo" -> "2020-01-01",
              "items" -> Json.arr(
                Json.obj("dueDate" -> "2020-10-25")
              ),
              "outstandingAmount" -> -1000,
              "periodKey" -> "#006"
            ),
            Json.obj(
              "mainType" -> "VAT POA Return Charge",
              "chargeType" -> "VAT POA Return Debit Charge",
              "taxPeriodFrom" -> "2020-12-01",
              "taxPeriodTo" -> "2021-01-01",
              "items" -> Json.arr(
                Json.obj("dueDate" -> "2021-10-25")
              ),
              "outstandingAmount" -> 1000,
              "periodKey" -> "#007"
            ),
            Json.obj(
              "mainType" -> "VAT POA Return Charge",
              "chargeType" -> "VAT POA Return Credit Charge",
              "taxPeriodFrom" -> "2021-12-01",
              "taxPeriodTo" -> "2022-01-01",
              "items" -> Json.arr(
                Json.obj("dueDate" -> "2022-10-25")
              ),
              "outstandingAmount" -> -1000,
              "periodKey" -> "#008"
            )
          )
        ), Map.empty[String, Seq[String]]
      )

      val expected = Right(Payments(Seq(
        Payment(
          "VAT Return Debit Charge",
          periodFrom = LocalDate.parse("2016-12-01"),
          periodTo = LocalDate.parse("2017-01-01"),
          due = LocalDate.parse("2017-10-25"),
          outstandingAmount = BigDecimal(1000.00),
          periodKey = "#003"
        ),
        Payment(
          "VAT Return Credit Charge",
          periodFrom = LocalDate.parse("2017-12-01"),
          periodTo = LocalDate.parse("2018-01-01"),
          due = LocalDate.parse("2018-10-25"),
          outstandingAmount = BigDecimal(-1000.00),
          periodKey = "#004"
        ),
        Payment(
          "VAT AA Return Debit Charge",
          periodFrom = LocalDate.parse("2018-12-01"),
          periodTo = LocalDate.parse("2019-01-01"),
          due = LocalDate.parse("2019-10-25"),
          outstandingAmount = BigDecimal(1000.00),
          periodKey = "#005"
        ),
        Payment(
          "VAT AA Return Credit Charge",
          periodFrom = LocalDate.parse("2019-12-01"),
          periodTo = LocalDate.parse("2020-01-01"),
          due = LocalDate.parse("2020-10-25"),
          outstandingAmount = BigDecimal(-1000.00),
          periodKey = "#006"
        ),
        Payment(
          "VAT POA Return Debit Charge",
          periodFrom = LocalDate.parse("2020-12-01"),
          periodTo = LocalDate.parse("2021-01-01"),
          due = LocalDate.parse("2021-10-25"),
          outstandingAmount = BigDecimal(1000.00),
          periodKey = "#007"
        ),
        Payment(
          "VAT POA Return Credit Charge",
          periodFrom = LocalDate.parse("2021-12-01"),
          periodTo = LocalDate.parse("2022-01-01"),
          due = LocalDate.parse("2022-10-25"),
          outstandingAmount = BigDecimal(-1000.00),
          periodKey = "#008"
        )
      )))

      val result = PaymentsReads.read("", "", httpResponse)

      "return a Payments instance" in {
        result shouldBe expected
      }
    }

    "the http response status is 200 OK but the JSON is invalid" should {

      val httpResponse = HttpResponse.apply(Status.OK,
        Json.obj(
          "financialTransactions" -> Json.arr(
            Json.obj()
          )
        ), Map.empty[String, Seq[String]]
      )

      val expected = Left(UnexpectedJsonFormat)

      val result = PaymentsReads.read("", "", httpResponse)

      "return a UnexpectedJsonFormat error" in {
        result shouldBe expected
      }
    }

    "the http response is 200 OK and there are no valid charge types" should {
      val httpResponse = HttpResponse.apply(Status.OK,
        Json.obj(
          "financialTransactions" -> Json.arr(
            Json.obj(
              "mainType" -> "VAT Return Charge",
              "chargeType" -> "Other Charge Type",
              "outstandingAmount" -> 99
            )
          )
        ), Map.empty[String, Seq[String]]
      )

      val expected = Right(Payments(Seq.empty))

      val result = PaymentsReads.read("", "", httpResponse)

      "return an empty Payments instance" in {
        result shouldBe expected
      }
    }

    "a http response of 400 BAD_REQUEST (single error)" should {

      val httpResponse = HttpResponse.apply(Status.BAD_REQUEST,
        Json.obj(
          "code" -> "INVALID DATE FROM",
          "reason" -> "Bad date from"
        ), Map.empty[String, Seq[String]]
      )

      val expected = Left(BadRequestError(
        code = "INVALID DATE FROM",
        errorResponse = "Bad date from"
      ))

      val result = PaymentsReads.read("", "", httpResponse)

      "return a BadRequestError" in {
        result shouldEqual expected
      }
    }

    "a http response of 400 BAD_REQUEST (multiple errors)" should {

      val httpResponse = HttpResponse.apply(Status.BAD_REQUEST,
        Json.obj(
          "failures" -> Json.arr(
            Json.obj(
              "code" -> "INVALID DATE FROM",
              "reason" -> "Bad date from"
            ),
            Json.obj(
              "code" -> "INVALID DATE TO",
              "reason" -> "Bad date to"
            )
          )
        ), Map.empty[String, Seq[String]]
      )

      val errors = Seq(ApiSingleError("INVALID DATE FROM", "Bad date from"), ApiSingleError("INVALID DATE TO", "Bad date to"))

      val expected = Left(MultipleErrors(Status.BAD_REQUEST.toString, Json.toJson(errors).toString()))

      val result = PaymentsReads.read("", "", httpResponse)

      "return a MultipleErrors" in {
        result shouldBe expected
      }
    }

    "a http response of 400 BAD_REQUEST (unknown API error json)" should {

      val httpResponse = HttpResponse.apply(Status.BAD_REQUEST,
        Json.obj(
          "foo" -> "ERROR",
          "bar" -> "unknown_error"
        ), Map.empty[String, Seq[String]]
      )

      val expected = Left(UnknownError)

      val result = PaymentsReads.read("", "", httpResponse)

      "return a UnknownError" in {
        result shouldEqual expected
      }
    }

    "a http response of 404 NOT_FOUND" should {

      val httpResponse = HttpResponse.apply(Status.NOT_FOUND, "", Map.empty[String, Seq[String]])

      val expected = Right(Payments(Seq.empty))

      val result = PaymentsReads.read("", "", httpResponse)

      "return a UnknownError" in {
        result shouldEqual expected
      }
    }

    "the HTTP response status is 5xx" should {

      val body: JsObject = Json.obj(
        "code" -> "GATEWAY_TIMEOUT",
        "reason" -> "GATEWAY_TIMEOUT"
      )

      val httpResponse = HttpResponse.apply(Status.GATEWAY_TIMEOUT, body, Map.empty[String, Seq[String]])
      val expected = Left(ServerSideError(Status.GATEWAY_TIMEOUT.toString, httpResponse.body))
      val result = PaymentsReads.read("", "", httpResponse)

      "return a ServerSideError" in {
        result shouldBe expected
      }
    }

    "the HTTP response status isn't handled" should {

      val body: JsObject = Json.obj(
        "code" -> "Conflict",
        "reason" -> "CONFLICT"
      )

      val httpResponse = HttpResponse.apply(Status.CONFLICT, body, Map.empty[String, Seq[String]])
      val expected = Left(UnexpectedStatusError(Status.CONFLICT.toString, httpResponse.body))
      val result = PaymentsReads.read("", "", httpResponse)

      "return an UnexpectedStatusError" in {
        result shouldBe expected
      }
    }
  }
}