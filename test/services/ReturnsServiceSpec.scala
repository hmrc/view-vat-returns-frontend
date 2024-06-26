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

package services

import connectors.httpParsers.ResponseHttpParsers.HttpResult

import java.time.LocalDate
import connectors.{FinancialDataConnector, VatReturnsConnector}
import controllers.ControllerBaseSpec
import models.Obligation.Status
import models.errors.{ObligationError, ServerSideError}
import models.payments.{Payment, Payments}
import models._
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ReturnsServiceSpec extends ControllerBaseSpec {

    val mockVatReturnsConnector: VatReturnsConnector = mock[VatReturnsConnector]
    val mockFinancialDataApiConnector: FinancialDataConnector = mock[FinancialDataConnector]
    val service = new ReturnsService(
      mockVatObligationsConnector,
      mockFinancialDataApiConnector,
      mockVatReturnsConnector
    )

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

    val examplePayment: Payment = Payment(
      "VAT Return Debit Charge",
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-02-01"),
      LocalDate.parse("2017-02-02"),
      5000,
      "#003"
    )

    val exampleObligations: VatReturnObligations = VatReturnObligations(
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

  val examplePayments: Payments = Payments(Seq(examplePayment))

  def callFinancialDataConnector(response: HttpResult[Payments]): Any =
    (mockFinancialDataApiConnector.getPayments(_: String, _: Option[Int])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returns(Future.successful(response))


  "Calling .getVatReturn" should {

    "return a VAT Return" in {
      (mockVatReturnsConnector.getVatReturnDetails(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(Future.successful(Right(exampleVatReturn)))

      lazy val result: ServiceResponse[VatReturn] = await(service.getVatReturn(vrn, "#001"))

      result shouldBe Right(exampleVatReturn)
    }
  }

  "Calling .getReturnObligationsForYear" when {

    "the obligations connector returns a successful response" should {

      "return all of a user's VAT return obligations" in {

        lazy val result: ServiceResponse[VatReturnObligations] = {
          callObligationsConnector(Right(exampleObligations))
          await(service.getReturnObligationsForYear(vrn, 2017, Status.All))
        }

        result shouldBe Right(exampleObligations)
      }
    }

    "the obligations connector returns an error" should {

      "return an ObligationError" in {

        lazy val result: ServiceResponse[VatReturnObligations] = {
          callObligationsConnector(Left(ServerSideError("ERROR", "ERROR")))
          await(service.getReturnObligationsForYear(vrn, 2018, Status.All))
        }

        result shouldBe Left(ObligationError)
      }
    }
  }

  "Calling .filterObligationsByDueDate" should {

    "when supplied with no obligations with end dates in the requested year" should {

      val date = LocalDate.parse("2016-10-10")
      val obligation = VatReturnObligation(date, date, date, "O", None, "")
      val obligations = VatReturnObligations(Seq(obligation, obligation))

      "return an empty sequence of obligations" in {
        lazy val result: VatReturnObligations = service.filterObligationsByDueDate(obligations, 2017)
        result shouldEqual VatReturnObligations(Seq())
      }
    }

    "when supplied with obligations with end dates in the requested year" should {

      val date = LocalDate.parse("2017-10-10")
      val obligation = VatReturnObligation(date, date, date, "O", None, "")
      val obligations = VatReturnObligations(Seq(obligation, obligation))

      "return an sequence containing obligations" in {
        lazy val result: VatReturnObligations = service.filterObligationsByDueDate(obligations, 2017)
        result shouldEqual VatReturnObligations(Seq(obligation, obligation))
      }
    }
  }

  "Calling .getPayment" when {

    "supplying without a year" should {

      "return all of a user's open payments" in {

        callFinancialDataConnector(Right(examplePayments))

        lazy val result: Option[Payment] = await(service.getPayment(vrn, "#003"))

        result shouldBe Some(examplePayment)
      }
    }

    "supplying with a year" should {

      "return all of a user's open payments" in {

        callFinancialDataConnector(Right(examplePayments))

        lazy val result: Option[Payment] = await(service.getPayment(vrn, "#003", Some(2019)))

        result shouldBe Some(examplePayment)
      }
    }
  }

  "Calling .getObligationWithMatchingPeriodKey" should {

    val obligations: VatReturnObligations = VatReturnObligations(
      Seq(
        VatReturnObligation(
          LocalDate.parse("2017-01-01"),
          LocalDate.parse("2018-12-31"),
          LocalDate.parse("2018-01-31"),
          "F",
          Some(LocalDate.parse("2018-01-31")),
          "#001"
        ),
        VatReturnObligation(
          LocalDate.parse("2017-01-01"),
          LocalDate.parse("2018-12-31"),
          LocalDate.parse("2018-01-31"),
          "F",
          Some(LocalDate.parse("2018-01-31")),
          "#002"
        ),
        VatReturnObligation(
          LocalDate.parse("2017-01-01"),
          LocalDate.parse("2018-12-31"),
          LocalDate.parse("2018-01-31"),
          "F",
          Some(LocalDate.parse("2018-01-31")),
          "#003"
        )
      )
    )

    "return the obligation with the matching period key" in {

      val expected: VatReturnObligation = VatReturnObligation(
        LocalDate.parse("2017-01-01"),
        LocalDate.parse("2018-12-31"),
        LocalDate.parse("2018-01-31"),
        "F",
        Some(LocalDate.parse("2018-01-31")),
        "#001"
      )

      val result: Option[VatReturnObligation] = {
        callObligationsConnector(Right(obligations))
        await(service.getObligationWithMatchingPeriodKey(vrn, 2018, "#001"))
      }
      result shouldBe Some(expected)
    }

    "return None" in {

      val result: Option[VatReturnObligation] = {
        callObligationsConnector(Right(obligations))
        await(service.getObligationWithMatchingPeriodKey(vrn, 2018, "#004"))
      }
      result shouldBe None
    }
  }

  "Calling the .constructReturnDetailsModel function" should {

    "create a VatReturnDetails object" in {

      val expected: VatReturnDetails = VatReturnDetails(
        exampleVatReturn, moneyOwed = true, oweHmrc = Some(true), Some(examplePayment)
      )
      val result: VatReturnDetails = service.constructReturnDetailsModel(exampleVatReturn, Some(examplePayment))

      result shouldBe expected
    }
  }

  "Calling .getLastObligation" when {

    val olderObligation: VatReturnObligation = VatReturnObligation(
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2018-12-31"),
      due = LocalDate.parse("2018-01-30"),
      "F",
      Some(LocalDate.parse("2018-01-31")),
      "#001"
    )
    val newerObligation: VatReturnObligation = VatReturnObligation(
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2018-12-31"),
      due = LocalDate.parse("2018-01-31"),
      "F",
      Some(LocalDate.parse("2018-01-31")),
      "#001"
    )

    "supplying multiple obligations" should {

      val obligations: Seq[VatReturnObligation] = Seq(olderObligation, olderObligation, newerObligation)

      "return the most recent obligation by due date" in {
        val result: VatReturnObligation = service.getLastObligation(obligations)
        result shouldEqual newerObligation
      }
    }

    "supplying one obligation" should {

      val obligations: Seq[VatReturnObligation] = Seq(olderObligation)

      "return the most recent obligation by due date" in {
        val result: VatReturnObligation = service.getLastObligation(obligations)
        result shouldEqual olderObligation
      }
    }
  }

  "Calling .getPreviousFulfilledObligations" should {

    "return obligations" in {

      lazy val result: ServiceResponse[VatReturnObligations] = {
        callObligationsConnector(Right(exampleObligations))
        await(service.getFulfilledObligations(vrn, LocalDate.parse("2018-01-01")))
      }

      result shouldBe Right(exampleObligations)
    }
  }
}
