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

import connectors.httpParsers.VatReturnHttpParser.VatReturnReads
import controllers.ControllerBaseSpec
import models.VatReturn
import models.errors._
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HttpResponse

class VatReturnHttpParserSpec extends ControllerBaseSpec {

  "VatReturnReads" when {

    "the HTTP response is OK (200)" should {

      val httpResponse = HttpResponse.apply(Status.OK, Json.obj(
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
      ), Map.empty[String, Seq[String]])

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

      val httpResponse = HttpResponse.apply(Status.BAD_REQUEST,
        Json.obj(
          "code" -> "INVALID",
          "message" -> "Fail!"
        ), Map.empty[String, Seq[String]]
      )

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

      val httpResponse = HttpResponse.apply(Status.BAD_REQUEST,
        Json.obj(
          "code" -> "400",
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
        ), Map.empty[String, Seq[String]]
      )

      val errors = Seq(ApiSingleError("INVALID", "Fail!"), ApiSingleError("INVALID_2", "Fail!"))

      val expected = Left(MultipleErrors("400", Json.toJson(errors).toString()))

      val result = VatReturnReads.read("", "", httpResponse)

      "return a MultipleErrors" in {
        result shouldBe expected
      }
    }

    "the HTTP response status is BAD_REQUEST (400) (unknown error)" should {

      val httpResponse = HttpResponse.apply(Status.BAD_REQUEST,
        Json.obj(
          "foo" -> "RED_CAR",
          "bar" -> "Fail!"
        ), Map.empty[String, Seq[String]]
      )

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

      val httpResponse = HttpResponse.apply(Status.GATEWAY_TIMEOUT, body, Map.empty[String, Seq[String]])
      val expected = Left(ServerSideError("504", httpResponse.body))
      val result = VatReturnReads.read("", "", httpResponse)

      "return a ServerSideError" in {
        result shouldBe expected
      }
    }

    "the HTTP response status isn't handled" should {

      val body: JsObject = Json.obj(
        "code" -> "Conflict",
        "message" -> "CONFLICT"
      )

      val httpResponse = HttpResponse.apply(Status.CONFLICT, body, Map.empty[String, Seq[String]])
      val expected = Left(UnexpectedStatusError("409", httpResponse.body))
      val result = VatReturnReads.read("", "", httpResponse)

      "return an UnexpectedStatusError" in {
        result shouldBe expected
      }
    }
  }
}
