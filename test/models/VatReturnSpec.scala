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

package models

import java.time.LocalDate
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class VatReturnSpec extends UnitSpec {

  "A VAT Return" should {

    val exampleVatReturn = VatReturn(
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
    )

    val exampleString =
      """{
        |"startDate":"2017-01-01",
        |"endDate":"2017-03-31",
        |"dateSubmitted":"2017-04-06",
        |"dueDate":"2017-04-08",
        |"ukVatDue":1297,
        |"euVatDue":5755,
        |"totalVatDue":7052,
        |"totalVatReclaimed":5732,
        |"totalOwed":1320,
        |"totalSales":77656,
        |"totalCosts":765765,
        |"euTotalSales":55454,
        |"euTotalCosts":545645
        |}"""
        .stripMargin.replace("\n", "")

    "parse to JSON" in {
      val result = Json.toJson(exampleVatReturn).toString
      result shouldEqual exampleString
    }

    "be parsed from appropriate JSON" in {
      val result = Json.parse(exampleString).as[VatReturn]
      result shouldEqual exampleVatReturn
    }
  }
}
