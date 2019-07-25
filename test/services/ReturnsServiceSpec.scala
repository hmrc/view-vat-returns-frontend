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

import common.TestModels.customerInformationMax
import connectors.{FinancialDataConnector, VatReturnsConnector}
import controllers.ControllerBaseSpec
import models.{User, _}
import models.Obligation.Status
import models.payments.{Payment, Payments}
import models.errors.{MandationStatusError, ObligationError, ServerSideError, VatSubscriptionError}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class ReturnsServiceSpec extends ControllerBaseSpec {

  private trait Test {
    val mockVatReturnsConnector: VatReturnsConnector = mock[VatReturnsConnector]
    val mockFinancialDataApiConnector: FinancialDataConnector = mock[FinancialDataConnector]
    val service = new ReturnsService(
      mockVatObligationsConnector,
      mockFinancialDataApiConnector,
      mockVatReturnsConnector,
      mockVatSubscriptionConnector
    )
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val migrationDate: Option[String] = Some("2018-01-01")

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

  "Calling .getReturnObligationsForYear" when {

    "the obligations connector returns a successful response and the Status is Fulfilled" should {

      "return a user's VAT return obligations with a start date after they migrated to ETMP" in new Test {

        lazy val result: ServiceResponse[VatReturnObligations] = {
          callObligationsConnector(Right(exampleObligations))
          await(service.getReturnObligationsForYear(User(vrn), 2018, Status.Fulfilled))
        }

        result shouldBe Right(VatReturnObligations(Seq.empty))
      }
    }

    "the obligations connector returns a successful response and the Status is not Fulfilled" should {

      "return all of a user's VAT return obligations" in new Test {

        lazy val result: ServiceResponse[VatReturnObligations] = {
          callObligationsConnector(Right(exampleObligations))
          await(service.getReturnObligationsForYear(User(vrn), 2018, Status.All))
        }

        result shouldBe Right(exampleObligations)
      }
    }

    "the obligations connector returns an error" should {

      "return an ObligationError" in new Test {

        lazy val result: ServiceResponse[VatReturnObligations] = {
          callObligationsConnector(Left(ServerSideError("ERROR", "ERROR")))
          await(service.getReturnObligationsForYear(User(vrn), 2018, Status.All))
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
      override implicit val migrationDate: Option[String] = Some("2016-01-01")
      val expected = VatReturnObligation(
        LocalDate.parse("2017-01-01"),
        LocalDate.parse("2018-12-31"),
        LocalDate.parse("2018-01-31"),
        "F",
        Some(LocalDate.parse("2018-01-31")),
        "#001"
      )

      val result: Option[VatReturnObligation] = {
        callObligationsConnector(Right(obligations))
        await(service.getObligationWithMatchingPeriodKey(User("111111111"), 2018, "#001"))
      }
      result shouldBe Some(expected)
    }

    "return None" in new Test {

      val result: Option[VatReturnObligation] = {
        callObligationsConnector(Right(obligations))
        await(service.getObligationWithMatchingPeriodKey(User("111111111"), 2018, "#004"))
      }
      result shouldBe None
    }
  }

  "Calling the .constructReturnDetailsModel function" should {

    "create a VatReturnDetails object" in new Test {
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

      lazy val result: ServiceResponse[VatReturnObligations] = {
        callObligationsConnector(Right(exampleObligations))
        service.getFulfilledObligations(LocalDate.parse("2018-01-01"))
      }

      await(result) shouldBe Right(exampleObligations)
    }
  }


  "Calling the .getMandationStatus function" when {

    val mandationStatusReturned: MandationStatus = MandationStatus("3")

    "the user is not mandated" should {

      "return a status" in new Test {
        (mockVatSubscriptionConnector.getMandationStatusInfo(_: String)
        (_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Right(mandationStatusReturned)))
        val mandationStatusResponse: ServiceResponse[MandationStatus] = await(service.getMandationStatus("123456789"))
        mandationStatusResponse shouldBe Right(mandationStatusReturned)
      }
    }

    "the connector call fails" should {

      "return None" in new Test {
        (mockVatSubscriptionConnector.getMandationStatusInfo(_: String)
        (_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Left(ServerSideError("", ""))))
        val mandationStatusResponse: ServiceResponse[MandationStatus] = await(service.getMandationStatus("123456789"))

        mandationStatusResponse shouldBe Left(MandationStatusError)
      }
    }
  }

  "Calling .filterPreETMPObligations" when {

    "migrationDate is defined" should {

      "filter the obligations based on the migration date" in new Test {

        val result: Option[VatReturnObligations] =
          await(service.filterPreETMPObligations(exampleObligations, migrationDate, user))
        result shouldBe Some(VatReturnObligations(Seq.empty))
      }
    }

    "migrationDate is not defined" when {

      "the call to the VatSubscription connector is successful" should {

        "filter the obligations based on the migration date" in new Test {

          val result: Option[VatReturnObligations] = {
            callSubscriptionConnector(Right(customerInformationMax))
            await(service.filterPreETMPObligations(exampleObligations, None, user))
          }
          result shouldBe Some(VatReturnObligations(Seq.empty))
        }
      }

      "the call to VatSubscription connector is not successful" should {

        "return None" in new Test {

          val result: Option[VatReturnObligations] = {
            callSubscriptionConnector(Left(ServerSideError("ERROR", "ERROR")))
            await(service.filterPreETMPObligations(exampleObligations, None, user))
          }
          result shouldBe None
        }
      }
    }
  }
}
