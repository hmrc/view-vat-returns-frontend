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

package connectors.httpParsers

import java.time.LocalDate

import connectors.httpParsers.PaymentsHttpParser.PaymentsReads
import models.errors._
import models.payments.{Payment, Payments}
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec

class PaymentsHttpParserSpec extends UnitSpec {

  "PaymentsReads" when {


    "a http response of 200 (OK) is received" should {

      val httpResponse = HttpResponse(Status.OK, responseJson = Some(
        Json.obj(
          "idType" -> "VRN",
          "idNumber" -> "999973804",
          "regimeType" -> "VATC",
          "processingDate" -> "2018-04-17T08:55:22Z",
          "financialTransactions" -> Json.arr(
            Json.obj(
              "chargeType"-> "VAT Return Debit Charge",
              "mainType"-> "VAT Return Charge",
              "periodKey"-> "18AC",
              "periodKeyDescription" -> "March 2018",
              "taxPeriodFrom"-> "2017-06-01",
              "taxPeriodTo" -> "2017-10-01",
              "businessPartner" -> "0100113120",
              "contractAccountCategory" -> "33",
              "contractAccount" -> "091700000405",
              "contractObjectType" -> "ZVAT",
              "contractObject" -> "00000180000000000165",
              "sapDocumentNumber" -> "003030001189",
              "sapDocumentNumberItem" -> "0001",
              "chargeReference" -> "XJ002610110056",
              "mainTransaction" -> "4700",
              "subTransaction" -> "1174",
              "originalAmount" -> 10169.45,
              "outstandingAmount" -> 5000.00,
              "items" -> Json.arr(
                Json.obj(
                  "subItem" -> "000",
                  "dueDate" -> "2017-11-01",
                  "amount" -> 10169.45
                )
              )
            )
          )
        )))
      
      val expectedResult = Right(Payments(Seq(
        Payment(
          chargeType = "VAT Return Debit Charge",
          start = LocalDate.parse("2017-06-01"),
          end = LocalDate.parse("2017-10-01"),
          due = LocalDate.parse("2017-11-01"),
          outstandingAmount = BigDecimal(5000.00),
          clearedAmount = BigDecimal(0),
          periodKey = "18AC"
        )))
      )

      val result = PaymentsReads.read("", "", httpResponse)

      "return a Payments instance" in {
        result shouldBe expectedResult
      }

    }

    "a http response of 400 BAD_REQUEST (single error)" should {

      val httpResponse = HttpResponse(Status.BAD_REQUEST,
        responseJson = Some(Json.obj(
          "code" -> "INVALID DATE FROM",
          "reason" -> "Bad date from"
        ))
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

      val httpResponse = HttpResponse(Status.BAD_REQUEST,
        responseJson = Some(Json.obj(
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
        ))
      )

      val errors = Seq(ApiSingleError("INVALID DATE FROM", "Bad date from"), ApiSingleError("INVALID DATE TO", "Bad date to"))

      val expected = Left(MultipleErrors(Status.BAD_REQUEST.toString, Json.toJson(errors).toString()))

      val result = PaymentsReads.read("", "", httpResponse)

      "return a MultipleErrors" in {
        result shouldBe expected
      }
    }

    "a http response of 400 BAD_REQUEST (unknown API error json)" should {

      val httpResponse = HttpResponse(Status.BAD_REQUEST,
        responseJson = Some(Json.obj(
          "foo" -> "ERROR",
          "bar" -> "unknown_error"
        ))
      )

      val expected = Left(UnknownError)

      val result = PaymentsReads.read("", "", httpResponse)

      "return a UnknownError" in {
        result shouldEqual expected
      }
    }

    "a http response of 404 NOT_FOUND" should {

      val httpResponse = HttpResponse(Status.NOT_FOUND, None)

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

      val httpResponse = HttpResponse(Status.GATEWAY_TIMEOUT, Some(body))
      val expected = Left(ServerSideError(Status.GATEWAY_TIMEOUT.toString, httpResponse.body))
      val result = PaymentsReads.read("", "", httpResponse)

      "return a ServerSideError" in {
        result shouldBe expected
      }
    }

    "the HTTP response status isn't handled" should {

      val body: JsObject = Json.obj(
        "code" -> "Conflict",
        "reason" -> "CONFLCIT"
      )

      val httpResponse = HttpResponse(Status.CONFLICT, Some(body))
      val expected = Left(UnexpectedStatusError(Status.CONFLICT.toString, httpResponse.body))
      val result = PaymentsReads.read("", "", httpResponse)

      "return an UnexpectedStatusError" in {
        result shouldBe expected
      }
    }
  }
}