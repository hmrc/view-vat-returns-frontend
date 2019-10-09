/*
 * Copyright 2019 HM Revenue & Customs
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

class VatReturnObligationSpec extends UnitSpec {

  "An obligation" should {

    "parse to the correct json format" in {

      val input = VatReturnObligation(
        LocalDate.parse("2017-01-01"),
        LocalDate.parse("2017-12-31"),
        LocalDate.parse("2018-01-31"),
        "O",
        None,
        "#001"
      )

      val expected =
        """{"periodFrom":"2017-01-01","periodTo":"2017-12-31","due":"2018-01-31","status":"O","periodKey":"#001"}"""


      val result = Json.toJson(input).toString()

      result shouldEqual expected
    }

    "parse from json" in {

      val input =
        """{"start":"2017-02-02","end":"2017-11-11","due":"2018-01-31","status":"O","periodKey":"#003"}"""

      val expected = VatReturnObligation(
        LocalDate.parse("2017-02-02"),
        LocalDate.parse("2017-11-11"),
        LocalDate.parse("2018-01-31"),
        "O",
        None,
        "#003"
      )

      val result = Json.parse(input).as[VatReturnObligation]

      result shouldEqual expected

    }

  }

  "Obligations" should {

    "parse to the correct json" in {

      val input = VatReturnObligations(
        Seq(
          VatReturnObligation(
            LocalDate.parse("2017-01-01"),
            LocalDate.parse("2017-12-31"),
            LocalDate.parse("2018-01-31"),
            "O",
            None,
            "#001"
          )
        )
      )

      val expected =
        """{"obligations":[{"periodFrom":"2017-01-01","periodTo":"2017-12-31","due":"2018-01-31","status":"O","periodKey":"#001"}]}"""

      val result = Json.toJson(input).toString()

      result shouldEqual expected

    }

    "parse from json" in {

      val input =
        """{"obligations":[{"start":"2017-01-01","end":"2017-12-31","due":"2018-01-31","status":"O","periodKey":"#001"}]}"""

      val expected = VatReturnObligations(
        Seq(
          VatReturnObligation(
            LocalDate.parse("2017-01-01"),
            LocalDate.parse("2017-12-31"),
            LocalDate.parse("2018-01-31"),
            "O",
            None,
            "#001"
          )
        )
      )

      val result = Json.parse(input).as[VatReturnObligations]

      result shouldEqual expected

    }

  }

}

