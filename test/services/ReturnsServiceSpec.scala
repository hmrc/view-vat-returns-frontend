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

package services

import java.time.LocalDate

import connectors.{FinancialDataConnector, VatObligationsConnector, VatReturnsConnector, VatSubscriptionConnector}
import controllers.ControllerBaseSpec
import models._
import models.Obligation.Status
import models.payments.{Payment, Payments}
import models.User
import models.errors.{DirectDebitStatusError, MandationStatusError, ServerSideError}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class ReturnsServiceSpec extends ControllerBaseSpec {

  private trait Test {
    val mockVatObligationsConnector: VatObligationsConnector = mock[VatObligationsConnector]
    val mockVatReturnsConnector: VatReturnsConnector = mock[VatReturnsConnector]
    val mockFinancialDataApiConnector: FinancialDataConnector = mock[FinancialDataConnector]
    val mockVatSubscriptionApiConnector: VatSubscriptionConnector = mock[VatSubscriptionConnector]
    val service = new ReturnsService(mockVatObligationsConnector, mockFinancialDataApiConnector, mockVatReturnsConnector,mockVatSubscriptionApiConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()

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
      "VAT",
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-02-01"),
      LocalDate.parse("2017-02-02"),
      5000,
      0,
      "#003"
    )

    val exampleObligations = VatReturnObligations(
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
  }

  "Calling .getVatReturn" should {

    "return a VAT Return" in new Test {
      (mockVatReturnsConnector.getVatReturnDetails(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(Future.successful(Right(exampleVatReturn)))

      lazy val result: ServiceResponse[VatReturn] = await(service.getVatReturn(User("999999999"), "#001"))

      result shouldBe Right(exampleVatReturn)
    }
  }

  "Calling .getReturnObligationsForYear" should {

    "return all of a user's VAT return obligations" in new Test {
      (mockVatObligationsConnector.getVatReturnObligations(_: String, _: Option[LocalDate], _: Option[LocalDate], _: Status.Value)
                                                          (_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *, *)
        .returns(Future.successful(Right(exampleObligations)))

      lazy val result: ServiceResponse[VatReturnObligations] =
        await(service.getReturnObligationsForYear(User("999999999"), 2018, Status.All))

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

  "Calling .getPayment" when {

    "supplying without a year" should {

      "return all of a user's open payments" in new Test {
        val examplePayments: Payments = Payments(Seq(examplePayment))

        (mockFinancialDataApiConnector.getPayments(_: String, _: Option[Int])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returns(Future.successful(Right(examplePayments)))

        lazy val result: Option[Payment] = await(service.getPayment(User("111111111"), "#003"))

        result shouldBe Some(examplePayment)
      }
    }

    "supplying with a year" should {

      "return all of a user's open payments" in new Test {
        val examplePayments: Payments = Payments(Seq(examplePayment))

        (mockFinancialDataApiConnector.getPayments(_: String, _: Option[Int])(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returns(Future.successful(Right(examplePayments)))

        lazy val result: Option[Payment] = await(service.getPayment(User("111111111"), "#003", Some(2019)))

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

    "return the obligation with the matching period key" in new Test {
      val expected = VatReturnObligation(
        LocalDate.parse("2017-01-01"),
        LocalDate.parse("2018-12-31"),
        LocalDate.parse("2018-01-31"),
        "F",
        Some(LocalDate.parse("2018-01-31")),
        "#001"
      )

      (mockVatObligationsConnector.getVatReturnObligations(_: String, _: Option[LocalDate], _: Option[LocalDate], _: Status.Value)
                                                          (_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *, *)
        .returns(Future.successful(Right(obligations)))

      val result: Option[VatReturnObligation] = await(service.getObligationWithMatchingPeriodKey(User("111111111"), 2018, "#001"))
      result shouldBe Some(expected)
    }

    "return None" in new Test {
      (mockVatObligationsConnector.getVatReturnObligations(_: String, _: Option[LocalDate], _: Option[LocalDate], _: Status.Value)
                                                          (_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *, *)
        .returns(Future.successful(Right(obligations)))

      val result: Option[VatReturnObligation] = await(service.getObligationWithMatchingPeriodKey(User("111111111"), 2018, "#004"))
      result shouldBe None
    }
  }

  "Calling the .constructReturnDetailsModel function" should {

    "create a VatReturnDetails object" in new Test {
      val expected: VatReturnDetails = VatReturnDetails(
        exampleVatReturn, moneyOwed = true, isRepayment = false, Some(examplePayment)
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

      "return the most recent obligation by due date" in new Test {
        val result: VatReturnObligation = service.getLastObligation(obligations)
        result shouldEqual newerObligation
      }
    }

    "supplying one obligation" should {

      val obligations: Seq[VatReturnObligation] = Seq(olderObligation)

      "return the most recent obligation by due date" in new Test {
        val result: VatReturnObligation = service.getLastObligation(obligations)
        result shouldEqual olderObligation
      }
    }
  }

  "Calling .getPreviousFulfilledObligations" should {

    "return obligations" in new Test {
      implicit val user: User = User("999999999")

      (mockVatObligationsConnector.getVatReturnObligations(_: String, _: Option[LocalDate], _: Option[LocalDate], _: Status.Value)
                                                          (_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *, *, *)
        .returns(Future.successful(Right(exampleObligations)))

      lazy val result: ServiceResponse[VatReturnObligations] = service.getFulfilledObligations(LocalDate.parse("2018-01-01"))

      await(result) shouldBe Right(exampleObligations)
    }
  }

  "Calling the .getDirectDebitStatus function" when {

    "the user has a direct debit setup" should {

      "return a DirectDebitStatus with true" in new Test {
        (mockFinancialDataApiConnector.getDirectDebitStatus(_: String)
        (_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Right(true)))
        val paymentsResponse: ServiceResponse[Boolean] = await(service.getDirectDebitStatus("123456789"))

        paymentsResponse shouldBe Right(true)
      }
    }

    "the connector call fails" should {

      "return None" in new Test {
        (mockFinancialDataApiConnector.getDirectDebitStatus(_: String)
        (_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Left(ServerSideError("", ""))))
        val paymentsResponse: ServiceResponse[Boolean] = await(service.getDirectDebitStatus("123456789"))

        paymentsResponse shouldBe Left(DirectDebitStatusError)
      }
    }
  }

  "Calling the .getMandationStatus function" when {

    val mandationStatusReturned: MandationStatus = MandationStatus("3")

    "the user is not mandated" should {

      "return a status" in new Test {
        (mockVatSubscriptionApiConnector.getMandationStatusInfo(_: String)
        (_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Right(mandationStatusReturned)))
        val mandationStatusResponse: ServiceResponse[MandationStatus] = await(service.getMandationStatus("123456789"))
        mandationStatusResponse shouldBe Right(mandationStatusReturned)
      }
    }

    "the connector call fails" should {

      "return None" in new Test {
        (mockVatSubscriptionApiConnector.getMandationStatusInfo(_: String)
        (_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Left(ServerSideError("", ""))))
        val mandationStatusResponse: ServiceResponse[MandationStatus] = await(service.getMandationStatus("123456789"))

        mandationStatusResponse shouldBe Left(MandationStatusError)
      }
    }
  }
}
