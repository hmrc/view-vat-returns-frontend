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

import models.payments.{Payment, Payments}
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class PaymentsSpec extends UnitSpec {

  "A payment" should {

    val examplePayment = Payment(
      "VAT",
      LocalDate.parse("2017-06-01"),
      LocalDate.parse("2017-07-01"),
      LocalDate.parse("2017-07-21"),
      10000,
      "#004"
    )

    val exampleInputString =
      """{
        |"chargeType":"VAT",
        |"taxPeriodFrom":"2017-06-01",
        |"taxPeriodTo":"2017-07-01",
        |"items":[{"dueDate":"2017-07-21"}, {"dueDate":"2017-07-22"}],
        |"outstandingAmount":10000,
        |"periodKey":"#004"
        |}"""
        .stripMargin.replace("\n", "")

    "be parsed from appropriate JSON" in {
      val result = Json.parse(exampleInputString).as[Payment]
      result shouldEqual examplePayment
    }
  }

  "Payments" should {

    val examplePayments = Payments(
      Seq(
        Payment(
          "VAT",
          LocalDate.parse("2017-06-01"),
          LocalDate.parse("2017-07-01"),
          LocalDate.parse("2017-07-21"),
          10000,
          "#004"
        ),
        Payment(
          "VAT",
          LocalDate.parse("2017-07-01"),
          LocalDate.parse("2017-08-01"),
          LocalDate.parse("2017-08-21"),
          4000,
          "#005"
        )
      )
    )

    val exampleInputString =
      """{
        |"financialTransactions": [{
        |"chargeType":"VAT",
        |"taxPeriodFrom":"2017-06-01",
        |"taxPeriodTo":"2017-07-01",
        |"items":[{"dueDate":"2017-07-21"}, {"dueDate":"2017-07-22"}],
        |"outstandingAmount":10000,
        |"periodKey":"#004"
        |},{
        |"chargeType":"VAT",
        |"taxPeriodFrom":"2017-07-01",
        |"taxPeriodTo":"2017-08-01",
        |"items":[{"dueDate":"2017-08-21"}, {"dueDate":"2017-08-22"}],
        |"outstandingAmount":4000,
        |"periodKey":"#005"
        |}]}"""
        .stripMargin.replace("\n", "")

    "be parsed from appropriate JSON" in {
      val result = Json.parse(exampleInputString).as[Payments]
      result shouldEqual examplePayments
    }
  }
}
