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

package connectors

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationBaseSpec
import models.errors.BadRequestError
import models.payments.{Payment, Payments}
import stubs.FinancialDataStub
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class FinancialDataConnectorISpec extends IntegrationBaseSpec {

  private trait Test {
    def setupStubs(): StubMapping

    val connector: FinancialDataConnector = app.injector.instanceOf[FinancialDataConnector]
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "calling getPayments" when {

    "response contains payments" when {

      "not supplying a year" should {

        "return all open payments" in new Test {
          override def setupStubs(): StubMapping =
            FinancialDataStub.stubAllOutstandingPayments(Map("onlyOpenItems" -> "true"))

          val expected = Right(Payments(Seq(
            Payment(
              "VAT Return Debit Charge",
              LocalDate.parse("2018-05-01"),
              LocalDate.parse("2018-06-20"),
              LocalDate.parse("2018-06-21"),
              4000,
              "#001"
            ),
            Payment(
              "VAT Return Debit Charge",
              LocalDate.parse("2018-05-01"),
              LocalDate.parse("2018-06-20"),
              LocalDate.parse("2018-06-21"),
              0,
              "#002"
            )
          )))

          setupStubs()
          private val result = await(connector.getPayments("111111111", None))

          result shouldBe expected
        }
      }

      "supplying a year" should {

        "return all payments for 2019" in new Test {
          override def setupStubs(): StubMapping =
            FinancialDataStub.stubAllOutstandingPayments(Map("dateFrom" -> "2019-01-01", "dateTo" -> "2019-12-31"))

          val expected = Right(Payments(Seq(
            Payment(
              "VAT Return Debit Charge",
              LocalDate.parse("2018-05-01"),
              LocalDate.parse("2018-06-20"),
              LocalDate.parse("2018-06-21"),
              4000,
              "#001"
            ),
            Payment(
              "VAT Return Debit Charge",
              LocalDate.parse("2018-05-01"),
              LocalDate.parse("2018-06-20"),
              LocalDate.parse("2018-06-21"),
              0,
              "#002"
            )
          )))

          setupStubs()
          private val result = await(connector.getPayments("111111111", Some(2019)))

          result shouldBe expected
        }
      }
    }

    "response does not contain payments" should {

      "return an empty list of payments" in new Test {
        override def setupStubs(): StubMapping = FinancialDataStub.stubNoPayments

        val expected = Right(Payments(Seq.empty))

        setupStubs()
        private val result = await(connector.getPayments("111111111", None))

        result shouldBe expected
      }
    }

    "supplying with an invalid VRN" should {

      "return a BadRequestError" in new Test {
        override def setupStubs(): StubMapping = FinancialDataStub.stubInvalidVrn

        val expected = Left(BadRequestError(
          code = "INVALID_VRN",
          errorResponse = "VRN was invalid!"
        ))

        setupStubs()
        private val result = await(connector.getPayments("111", None))

        result shouldBe expected
      }
    }
  }

}
