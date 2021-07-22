/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json

class VatReturnSpec extends AnyWordSpecLike with Matchers {

  "A VAT Return" should {

    val exampleVatReturn = VatReturn(
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
    )

    val exampleString =
      """{
        |"periodKey":"#001",
        |"vatDueSales":1297,
        |"vatDueAcquisitions":5755,
        |"totalVatDue":7052,
        |"vatReclaimedCurrPeriod":5732,
        |"netVatDue":1320,
        |"totalValueSalesExVAT":77656,
        |"totalValuePurchasesExVAT":765765,
        |"totalValueGoodsSuppliedExVAT":55454,
        |"totalAcquisitionsExVAT":545645
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
