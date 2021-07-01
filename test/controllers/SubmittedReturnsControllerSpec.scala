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

package controllers

import java.time.LocalDate

import common.SessionKeys
import common.TestModels.{customerInformationMax, customerInformationMin, customerInformationNoMigDates, customerInformationNonMTDfB}
import models._
import models.errors.ObligationError
import models.viewModels.{ReturnObligationsViewModel, VatReturnsViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import views.html.errors.SubmittedReturnsErrorView
import views.html.returns.SubmittedReturnsView

import scala.concurrent.Future

class SubmittedReturnsControllerSpec extends ControllerBaseSpec {

  def exampleObligations(year: Int): Future[ServiceResponse[VatReturnObligations]] = Right(
    VatReturnObligations(Seq(VatReturnObligation(
      LocalDate.parse(s"$year-01-01"),
      LocalDate.parse(s"$year-12-31"),
      LocalDate.parse(s"$year-01-31"),
      "F",
      None,
      "#001"
    )))
  )

  val emptyObligations: ServiceResponse[VatReturnObligations] = Right(VatReturnObligations(Seq.empty))

  def controller: SubmittedReturnsController = new SubmittedReturnsController(
    mcc,
    mockVatReturnService,
    mockAuthorisedController,
    mockDateService,
    mockServiceInfoService,
    mockSubscriptionService,
    inject[SubmittedReturnsView],
    inject[SubmittedReturnsErrorView],
    ddInterruptPredicate
  )

  val exampleMigrationDateModel: MigrationDateModel = MigrationDateModel(Some(LocalDate.parse("2018-01-01")), None)

  "Calling the .redirect action" when {

    "authorised" should {

      lazy val result = {
        callAuthService(individualAuthResult)
        controller.redirect(2020)(fakeRequest.withSession(SessionKeys.insolventWithoutAccessKey -> "false", SessionKeys.futureInsolvencyDate -> "false"))
      }

      s"redirect the user to ${controllers.routes.SubmittedReturnsController.submittedReturns().url}" in {
        status(result) shouldBe MOVED_PERMANENTLY
        redirectLocation(result) shouldBe Some("/vat-through-software/vat-returns/submitted")
      }
    }

    "unauthorised" should {

      lazy val result = {
        callAuthService(Future.failed(InsufficientEnrolments()))
        controller.redirect(2020)(fakeRequest)
      }

      "return forbidden" in {
        status(result) shouldBe FORBIDDEN
      }
    }

    insolvencyCheck(controller.redirect(2020))
  }

  "Calling the .submittedReturns action" when {

    "a user is logged in and enrolled to HMRC-MTD-VAT" when {

      "the user's VAT registration date is in the previous year (2017)" should {

        lazy val result = {
          callDateService()
          callExtendedAudit
          callAuthService(individualAuthResult)
          callObligationsForYear(exampleObligations(2018))
          callObligationsForYear(exampleObligations(2017))
          callServiceInfoPartialService
          callSubscriptionService(Some(customerInformationMax))
          controller.submittedReturns(request())
        }

        "return 200" in {
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
        }

        "return charset of utf-8" in {
          charset(result) shouldBe Some("utf-8")
        }

        "return tabs for current year (2018) and previous year (2017) but not Previous Returns" in {
          val body = await(bodyOf(result))
          body should include ("""<a class="govuk-tabs__tab" href="#year-2018"""")
          body should include ("""<a class="govuk-tabs__tab" href="#year-2017"""")
          body shouldNot include ("""<a class="govuk-tabs__tab" href="#previous-returns"""")
        }
      }

      "the user's VAT registration date is in the current year (2018)" should {

        lazy val result = {
          callDateService()
          callExtendedAudit
          callAuthService(individualAuthResult)
          callObligationsForYear(exampleObligations(2018))
          callServiceInfoPartialService
          callSubscriptionService(Some(customerInformationMax.copy(effectiveRegistrationDate = Some("2018-01-01"))))
          controller.submittedReturns(request())
        }

        "return 200" in {
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
        }

        "return charset of utf-8" in {
          charset(result) shouldBe Some("utf-8")
        }

        "return a tab for current year (2018) but not previous year (2017) or Previous Returns" in {
          val body = await(bodyOf(result))
          body should include ("""<a class="govuk-tabs__tab" href="#year-2018"""")
          body shouldNot include ("""<a class="govuk-tabs__tab" href="#year-2017"""")
          body shouldNot include ("""<a class="govuk-tabs__tab" href="#previous-returns"""")
        }
      }

      "the user has the VATDEC enrolment and no migration dates" should {

        lazy val result = {
          callDateService()
          callExtendedAudit
          callAuthService(migratedUserAuthResult)
          callObligationsForYear(exampleObligations(2018))
          callObligationsForYear(exampleObligations(2017))
          callServiceInfoPartialService
          callSubscriptionService(Some(customerInformationNoMigDates))
          controller.submittedReturns(request())
        }

        "return 200" in {
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
        }

        "return charset of utf-8" in {
          charset(result) shouldBe Some("utf-8")
        }

        "return a tab for Previous Returns" in {
          val body = await(bodyOf(result))
          body should include ("""<a class="govuk-tabs__tab" href="#previous-returns"""")
        }
      }
    }

    "an agent logs into the service" when {

      "the agent has access" should {

        lazy val result = {
          callDateService()
          callExtendedAudit
          callAuthService(agentAuthResult)
          callAuthServiceEnrolmentsOnly(Enrolments(agentEnrolment))
          callObligationsForYear(exampleObligations(2018))
          callObligationsForYear(exampleObligations(2017))
          callSubscriptionService(Some(customerInformationMax))
          callSubscriptionService(Some(customerInformationMax))
          controller.submittedReturns(request(fakeRequestWithClientsVRN))
        }

        "return 200" in {
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
        }

        "return charset of utf-8" in {
          charset(result) shouldBe Some("utf-8")
        }
      }

    }

    "a user is not authorised" should {

      lazy val result = {
        callAuthService(Future.failed(InsufficientEnrolments()))
        controller.submittedReturns(request())
      }

      "return 403 (Forbidden)" in {
        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "a user is not authenticated" should {

      lazy val result = {
        callAuthService(Future.failed(MissingBearerToken()))
        controller.submittedReturns(request())
      }

      "return 303 (SEE_OTHER)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to sign in" in {
        redirectLocation(result) shouldBe Some(mockConfig.signInUrl)
      }
    }

    "an error occurs upstream" should {

      lazy val result: Result = {
        callAuthService(individualAuthResult)
        callDateService()
        callServiceInfoPartialService
        callObligationsForYear(Left(ObligationError))
        callObligationsForYear(Left(ObligationError))
        callSubscriptionService(Some(customerInformationMax))
        controller.submittedReturns(request())
      }

      "return 500" in {
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "return the standard error view" in {
        val document: Document = Jsoup.parse(bodyOf(result))
        messages(document.select("h1").first().text()) shouldBe "Sorry, there is a problem with the service"
      }
    }

    insolvencyCheck(controller.submittedReturns)

    lazy val result =  {
      callAuthService(individualAuthResult)
      callSubscriptionService(Some(customerInformationNonMTDfB))
      callDateService()
      controller.submittedReturns()(DDInterruptRequest)
    }
    "return a 303" in {
      status(result) shouldBe Status.SEE_OTHER
    }
    "check the redirect location" in {
      redirectLocation(result) shouldBe Some(mockConfig.vatSummaryBase + "/vat-through-software/direct-debit-interrupt?redirectUrl="
        + mockConfig.selfHost + "/homepage")
    }
  }

  "Calling .getValidYears" when {

    "the migration date is in the current year" should {

      lazy val result = {
        callDateService()
        controller.getValidYears(Some(LocalDate.parse("2018-01-01")))
      }

      "return a sequence containing one service call" in {
        await(result) shouldBe Seq(2018)
      }
    }

    "the migration date is last year" should {

      lazy val result = {
        callDateService()
        controller.getValidYears(Some(LocalDate.parse("2017-01-01")))
      }

      "return a sequence containing one service call" in {
        await(result) shouldBe Seq(2018, 2017)
      }
    }

    "the migration date is before last year" should {

      lazy val result = {
        callDateService()
        controller.getValidYears(Some(LocalDate.parse("2016-01-01")))
      }

      "return a sequence containing one service call" in {
        await(result) shouldBe Seq(2018, 2017, 2016)
      }
    }
  }

  "Calling .getReturnObligations" when {

    "the obligation calls are successful" should {

      lazy val result = {
        callDateService()
        callObligationsForYear(exampleObligations(2018))
        callExtendedAudit
        controller.getReturnObligations(exampleMigrationDateModel)
      }

      val expectedModel = VatReturnsViewModel(
        Seq(2018),
        Seq(ReturnObligationsViewModel(
          LocalDate.parse("2018-01-01"),
          LocalDate.parse("2018-12-31"),
          "#001"
        )),
        showPreviousReturnsTab = false,
        vrn
      )

      "return a VatReturnsViewModel with the correct information" in {
        await(result) shouldBe Right(expectedModel)
      }
    }

    "at least one call to the obligations service is unsuccessful" should {

      lazy val result = {
        callDateService()
        callObligationsForYear(Left(ObligationError))
        controller.getReturnObligations(exampleMigrationDateModel)
      }

      "return an ObligationError" in {
        await(result) shouldBe Left(ObligationError)
      }
    }
  }

  "Calling .customerMigratedWithin15M" when {

    "the interval between dates is less than 15 months" should {

      "return true" in {
        callDateService()
        controller.customerMigratedWithin15M(Some(LocalDate.parse("2017-02-02"))) shouldBe true
      }
    }

    "the interval between dates is 15 months or greater" should {

      "return false" in {
        callDateService()
        controller.customerMigratedWithin15M(Some(LocalDate.parse("2017-02-01"))) shouldBe false
      }
    }

    "the interval is 0 days" should {

      "return true" in {
        callDateService()
        controller.customerMigratedWithin15M(Some(LocalDate.parse("2018-05-01"))) shouldBe true
      }
    }

    "the date provided is None" should {

      "return false as a default" in {
        controller.customerMigratedWithin15M(None) shouldBe false
      }
    }
  }

  "Calling .getMigrationDates" when {

    "the account details service returns both migration dates" should {

      lazy val result = {
        controller.getMigrationDates(Some(customerInformationMax))
      }

      "return the correct date model" in {
        await(result) shouldBe MigrationDateModel(Some(LocalDate.parse("2017-01-01")), Some(LocalDate.parse("2018-02-02")))
      }
    }

    "the account details service returns no migration dates" should {

      lazy val result = {
        controller.getMigrationDates(Some(customerInformationMin))
      }

      "return the correct date model" in {
        await(result) shouldBe MigrationDateModel(None, None)
      }
    }

    "the account details service returns a failure" should {

      lazy val result = {
        controller.getMigrationDates(None)
      }

      "return the correct date model" in {
        await(result) shouldBe MigrationDateModel(None, None)
      }
    }
  }
}
