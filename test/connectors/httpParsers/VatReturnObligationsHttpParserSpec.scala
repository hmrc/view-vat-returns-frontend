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

import connectors.httpParsers.VatReturnObligationsHttpParser.VatReturnObligationsReads
import controllers.ControllerBaseSpec
import models.errors._
import models.{VatReturnObligation, VatReturnObligations}
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.HttpResponse

class VatReturnObligationsHttpParserSpec extends ControllerBaseSpec {

  "VatReturnObligationsReads" when {

    "the HTTP response status is OK (200) and JSON is valid" should {

      val httpResponse = HttpResponse.apply(Status.OK,
        Json.obj(
          "obligations" -> Json.arr(
            Json.obj(
              "start" -> "2017-01-01",
              "end" -> "2017-03-30",
              "due" -> "2017-04-30",
              "status" -> "O",
              "periodKey" -> "#001"
            )
          )
        ), Map.empty[String, Seq[String]]
      )

      val expected = Right(VatReturnObligations(Seq(
        VatReturnObligation(
          periodFrom = LocalDate.parse("2017-01-01"),
          periodTo = LocalDate.parse("2017-03-30"),
          due = LocalDate.parse("2017-04-30"),
          status = "O",
          received = None,
          periodKey = "#001"
        )
      )))

      val result = VatReturnObligationsReads.read("", "", httpResponse)

      "return a VatReturnObligations" in {
        result shouldBe expected
      }
    }

    "the http response status is 200 OK but the JSON is invalid" should {

      val httpResponse = HttpResponse.apply(Status.OK,
        Json.obj(
          "obligations" -> Json.arr(
            Json.obj()
          )
        ), Map.empty[String, Seq[String]]
      )

      val expected = Left(UnexpectedJsonFormat)

      val result = VatReturnObligationsReads.read("", "", httpResponse)

      "return a UnexpectedJsonFormat error" in {
        result shouldBe expected
      }
    }

    "the HTTP response status is BAD_REQUEST (400) (single error)" should {

      val httpResponse = HttpResponse.apply(Status.BAD_REQUEST,
        Json.obj(
          "code" -> "VRN_INVALID",
          "message" -> "Fail!"
        ), Map.empty[String, Seq[String]]
      )

      val expected = Left(BadRequestError(
        code = "VRN_INVALID",
        errorResponse = "Fail!"
      ))

      val result = VatReturnObligationsReads.read("", "", httpResponse)

      "return a BadRequestError" in {
        result shouldBe expected
      }
    }

    "the HTTP response status is BAD_REQUEST (400) (multiple errors)" should {

      val httpResponse: AnyRef with HttpResponse = HttpResponse.apply(Status.BAD_REQUEST,
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

      val result = VatReturnObligationsReads.read("", "", httpResponse)

      "return a MultipleErrors" in {
        result shouldBe expected
      }
    }

    "the HTTP response status is NOT_FOUND (404)" should {

      val httpResponse: AnyRef with HttpResponse = HttpResponse.apply(Status.NOT_FOUND, "", Map.empty[String, Seq[String]])
      val expected = Right(VatReturnObligations(Seq.empty))

      val result = VatReturnObligationsReads.read("", "", httpResponse)

      "return an empty sequence of obligations" in {
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

      val result = VatReturnObligationsReads.read("", "", httpResponse)

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
      val expected = Left(ServerSideError(Status.GATEWAY_TIMEOUT.toString, httpResponse.body))
      val result = VatReturnObligationsReads.read("", "", httpResponse)

      "return a ServerSideError" in {
        result shouldBe expected
      }
    }

    "the HTTP response status isn't handled" should {

      val body: JsObject = Json.obj(
        "code" -> "Conflict",
        "message" -> "CONFLCIT"
      )

      val httpResponse = HttpResponse.apply(Status.CONFLICT, body, Map.empty[String, Seq[String]])
      val expected = Left(UnexpectedStatusError(Status.CONFLICT.toString, httpResponse.body))
      val result = VatReturnObligationsReads.read("", "", httpResponse)

      "return an UnexpectedStatusError" in {
        result shouldBe expected
      }
    }
  }
}
