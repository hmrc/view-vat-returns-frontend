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

import connectors.httpParsers.VatReturnHttpParser.VatReturnReads
import models.VatReturn
import models.errors.{BadRequestError, MultipleErrors, ServerSideError, UnexpectedStatusError, UnknownError}
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec

class VatReturnHttpParserSpec extends UnitSpec {

  "VatReturnReads" when {

    "the HTTP response is OK (200)" should {

      val httpResponse = HttpResponse(Status.OK, responseJson = Some(Json.obj(
        "periodKey" -> "#001",
        "vatDueSales" -> 1297,
        "vatDueAcquisitions" -> 5755,
        "totalVatDue" -> 7052,
        "vatReclaimedCurrPeriod" -> 5732,
        "netVatDue" -> 1320,
        "totalValueSalesExVAT" -> 77656,
        "totalValuePurchasesExVAT" -> 765765,
        "totalValueGoodsSuppliedExVAT" -> 55454,
        "totalAcquisitionsExVAT" -> 545645
      )))

      val expected = Right(VatReturn(
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
      ))

      val result = VatReturnReads.read("", "", httpResponse)

      "return a VatReturn" in {
        result shouldBe expected
      }
    }

    "the HTTP response status is BAD_REQUEST (400) (single error)" should {

      val httpResponse = HttpResponse(Status.BAD_REQUEST, responseJson = Some(
        Json.obj(
          "code" -> "INVALID",
          "message" -> "Fail!"
        )
      ))

      val expected = Left(BadRequestError(
        code = "INVALID",
        errorResponse = "Fail!"
      ))

      val result = VatReturnReads.read("", "", httpResponse)

      "return a BadRequestError" in {
        result shouldBe expected
      }
    }

    "the HTTP response status is BAD_REQUEST (400) (multiple errors)" should {

      val httpResponse = HttpResponse(Status.BAD_REQUEST, responseJson = Some(
        Json.obj(
          "code" -> "BAD_REQUEST",
          "message" -> "Fail!",
          "errors" -> Json.arr(
            Json.obj(
              "code" -> "INVALID",
              "message" -> "Fail!"
            ),
            Json.obj(
              "code" -> "INVALID_2",
              "message" -> "Fail!"
            )
          )
        )
      ))

      val expected = Left(MultipleErrors)

      val result = VatReturnReads.read("", "", httpResponse)

      "return a MultipleErrors" in {
        result shouldBe expected
      }
    }

    "the HTTP response status is BAD_REQUEST (400) (unknown error)" should {

      val httpResponse = HttpResponse(Status.BAD_REQUEST, responseJson = Some(
        Json.obj(
          "foo" -> "RED_CAR",
          "bar" -> "Fail!"
        )
      ))

      val expected = Left(UnknownError)

      val result = VatReturnReads.read("", "", httpResponse)

      "return an UnknownError" in {
        result shouldBe expected
      }
    }

    "the HTTP response status is 5xx" should {

      val body: JsObject = Json.obj(
        "code" -> "GATEWAY_TIMEOUT",
        "message" -> "GATEWAY_TIMEOUT"
      )

      val httpResponse = HttpResponse(Status.GATEWAY_TIMEOUT, Some(body))
      val expected = Left(ServerSideError(Status.GATEWAY_TIMEOUT, httpResponse.body))
      val result = VatReturnReads.read("", "", httpResponse)

      "return a ServerSideError" in {
        result shouldBe expected
      }
    }

    "the HTTP response status isn't handled" should {

      val body: JsObject = Json.obj(
        "code" -> "Conflict",
        "message" -> "CONFLCIT"
      )

      val httpResponse = HttpResponse(Status.CONFLICT, Some(body))
      val expected = Left(UnexpectedStatusError(Status.CONFLICT, httpResponse.body))
      val result = VatReturnReads.read("", "", httpResponse)

      "return an UnexpectedStatusError" in {
        result shouldBe expected
      }
    }
  }
}
