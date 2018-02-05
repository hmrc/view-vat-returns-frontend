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

package connectors.httpParsers

import java.time.LocalDate

import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec

class PaymentsHttpParserSpec extends UnitSpec {

  "PaymentsReads" when {

    "a http response of 200 is received" should {

      val httpResponse = HttpResponse(Status.OK, responseJson = Some(
        Json.obj(
          "financialTransactions" -> Json.arr(
            Json.obj(
              "taxPeriodFrom" -> "2017-06-01",
              "taxPeriodTo" -> "2017-10-01",
              "items" -> Json.arr(
                Json.obj("dueDate" -> "2017-11-01")
              ),
              "outstandingAmount" -> 5000,
              "periodKey" -> "#004"
            )
          )
        )
      ))

      val expectedResult = Right(Payments(Seq(
        Payment(
          start = LocalDate.parse("2017-06-01"),
          end = LocalDate.parse("2017-10-01"),
          due = LocalDate.parse("2017-11-01"),
          outstandingAmount = BigDecimal(5000.00),
          periodKey = "#004"
        )))
      )

      val result = PaymentsReads.reads("","", httpResponse)

      "return a Payments instance" in {
        result shouldBe expectedResult
      }

    }

  }

}
