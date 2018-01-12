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

import connectors.httpParsers.VatReturnHttpParser.VatReturnReads
import models.VatReturn
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.test.UnitSpec

class VatReturnHttpParserSpec extends UnitSpec {

  "VatReturnReads" when {

    "the HTTP response is OK (200)" should {

      val httpResponse = HttpResponse(Status.OK, responseJson = Some(Json.obj(
        "startDate" -> "2017-01-01",
        "endDate" -> "2017-03-31",
        "dateSubmitted" -> "2017-04-06",
        "dueDate" -> "2017-04-08",
        "ukVatDue" -> 1297,
        "euVatDue" -> 5755,
        "totalVatDue" -> 7052,
        "totalVatReclaimed" -> 5732,
        "totalOwed" -> 1320,
        "totalSales" -> 77656,
        "totalCosts" -> 765765,
        "euTotalSales" -> 55454,
        "euTotalCosts" -> 545645
      )))

      val expected = Right(VatReturn(
        LocalDate.parse("2017-01-01"),
        LocalDate.parse("2017-03-31"),
        LocalDate.parse("2017-04-06"),
        LocalDate.parse("2017-04-08"),
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
  }
}
