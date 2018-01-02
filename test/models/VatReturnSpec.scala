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

    val exampleString =
      """{
        |"businessName":"ABC Clothing",
        |"startDate":"2017-01-01",
        |"endDate":"2017-03-31",
        |"dateSubmitted":"2017-04-06",
        |"dueDate":"2017-04-08",
        |"totalSales":99999,
        |"euSales":77777,
        |"vatChargedInUk":4444,
        |"vatChargedToEu":5555,
        |"totalCosts":999999,
        |"euCosts":9444444,
        |"totalVatCharged":9999,
        |"totalVatReclaimed":7777,
        |"owedToHmrc":999.54
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
