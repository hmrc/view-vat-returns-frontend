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

package services

import java.time.LocalDate

import connectors.httpParsers.ResponseHttpParsers.HttpGetResult
import connectors.{FinancialDataConnector, VatApiConnector}
import controllers.ControllerBaseSpec
import models.VatReturnObligation.Status
import models.payments.{Payment, Payments}
import models.{User, VatReturn, VatReturnObligation, VatReturnObligations}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits._

class ReturnsServiceSpec extends ControllerBaseSpec {

  private trait Test {
    val mockVatApiConnector: VatApiConnector = mock[VatApiConnector]
    val mockFinancialDataApiConnector: FinancialDataConnector = mock[FinancialDataConnector]
    val service = new ReturnsService(mockVatApiConnector, mockFinancialDataApiConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "Calling .getVatReturn" should {

    val exampleVatReturn: VatReturn = VatReturn(
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

    "return a VAT Return" in new Test {
      (mockVatApiConnector.getVatReturnDetails(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(Future.successful(Right(exampleVatReturn)))

      lazy val result: HttpGetResult[VatReturn] = await(
        service.getVatReturnDetails(User("999999999"), "#001")
      )

      result shouldBe Right(exampleVatReturn)
    }
  }

  "Calling .getReturnObligationsForYear" should {

    val exampleObligations: VatReturnObligations = VatReturnObligations(
      Seq(
        VatReturnObligation(
          LocalDate.parse("2017-01-01"),
          LocalDate.parse("2018-12-31"),
          LocalDate.parse("2018-01-31"),
          "O",
          None,
          "#001"
        )
      )
    )

    "return all of a user's VAT return obligations" in new Test {
      (mockVatApiConnector.getVatReturnObligations(_: String, _: LocalDate, _: LocalDate, _: Status.Value)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *, *)
        .returns(Future.successful(Right(exampleObligations)))

      lazy val result: HttpGetResult[VatReturnObligations] =
        await(service.getReturnObligationsForYear(User("999999999"), 2018))

      result shouldBe Right(exampleObligations)
    }
  }

  "Calling .filterObligationsByDueDate" should {

    "when supplied with no obligations with end dates in the requested year" should {

      val date = LocalDate.parse("2016-10-10")
      val obligation = VatReturnObligation(date, date, date, "O", None, "")
      val obligations = VatReturnObligations(Seq(obligation, obligation))

      "return an empty sequence of obligations" in new Test {
        lazy val result: VatReturnObligations = service.filterObligationsByDueDate(obligations, 2017)

        result shouldEqual VatReturnObligations(Seq())
      }
    }

    "when supplied with obligations with end dates in the requested year" should {

      val date = LocalDate.parse("2017-10-10")
      val obligation = VatReturnObligation(date, date, date, "O", None, "")
      val obligations = VatReturnObligations(Seq(obligation, obligation))

      "return an sequence containing obligations" in new Test {
        lazy val result: VatReturnObligations = service.filterObligationsByDueDate(obligations, 2017)

        result shouldEqual VatReturnObligations(Seq(obligation, obligation))
      }
    }
  }

  "Calling .getOpenPayments" should {

    val examplePayments: Payments = Payments(
      Seq(
        Payment(
          LocalDate.parse("2017-01-01"),
          LocalDate.parse("2017-02-01"),
          LocalDate.parse("2017-02-02"),
          5000,
          "#003"
        )
      )
    )

    val examplePayment: Payment = Payment(
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-02-01"),
      LocalDate.parse("2017-02-02"),
      5000,
      "#003"
    )

    "return all of a user's open payments" in new Test {
      (mockFinancialDataApiConnector.getOpenPayments(_: User)(_: HeaderCarrier, _:ExecutionContext))
        .expects(*,*,*)
        .returns(Future.successful(Right(examplePayments)))

      lazy val result: Option[Payment] = await(service.getPayment(User("111111111"), "#003"))

      result shouldBe Some(examplePayment)
    }
  }
}
