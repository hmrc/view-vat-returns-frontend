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

package controllers

import java.time.LocalDate

import common.SessionKeys
import models._
import models.errors.ObligationError
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.test.Helpers.{charset, contentType, _}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.{User => _, _}

import scala.concurrent.Future

class ReturnDeadlinesControllerSpec extends ControllerBaseSpec {

  val obligation = VatReturnObligation(
    LocalDate.parse("2017-01-01"),
    LocalDate.parse("2017-12-31"),
    LocalDate.parse("2018-01-31"),
    "O",
    None,
    "#001"
  )

  val exampleObligations: ServiceResponse[VatReturnObligations] = Right(VatReturnObligations(Seq(obligation)))

  val emptyObligations: ServiceResponse[VatReturnObligations] = Right(VatReturnObligations(Seq.empty))

  def controller: ReturnDeadlinesController = new ReturnDeadlinesController(
    messages,
    enrolmentsAuthService,
    mockVatReturnService,
    mockAuthorisedController,
    mockDateService,
    mockServiceInfoService,
    mockConfig,
    mockAuditService
  )

  "Calling the .returnDeadlines action" when {

    "A user is logged in and enrolled to HMRC-MTD-VAT" should {

      lazy val result = {
        callAuthService(individualAuthResult)
        callDateService()
        callOpenObligations(exampleObligations)
        callExtendedAudit
        callServiceInfoPartialService
        controller.returnDeadlines()(fakeRequest)
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

    "for a non-MTDfB user (with the submit return feature enabled)" when {

      "mandation status is in session" should {

        lazy val result = {
          mockConfig.features.submitReturnFeatures(true)
          callAuthService(individualAuthResult)
          callDateService()
          callOpenObligations(exampleObligations)
          callExtendedAudit
          callServiceInfoPartialService
          controller.returnDeadlines()(fakeRequest.withSession("mtdVatMandationStatus" -> "Non MTDfB"))
        }

        "return 200" in {
          status(result) shouldBe Status.OK
        }

        "return the opt-out return deadlines page" in {
          val document: Document = Jsoup.parse(bodyOf(result))
          document.getElementById("submit-return-link").text() shouldBe "Submit VAT Return"
        }
      }

      "mandation status is not in session" should {

        lazy val result = {
          mockConfig.features.submitReturnFeatures(true)
          callAuthService(individualAuthResult)
          callDateService()
          callOpenObligations(exampleObligations)
          callExtendedAudit
          callServiceInfoPartialService
          callMandationService(Right(MandationStatus("Non MTDfB")))
          controller.returnDeadlines()(fakeRequest)
        }

        "return 200" in {
          status(result) shouldBe Status.OK
        }

        "return the opt-out return deadlines page" in {
          val document: Document = Jsoup.parse(bodyOf(result))
          document.getElementById("submit-return-link").text() shouldBe "Submit VAT Return"
        }

        "put the mandation status in the session" in {
          session(result).get(SessionKeys.mtdVatMandationStatus) shouldBe Some("Non MTDfB")
        }
      }
    }

    "for an MTDfB user (with the submit return feature enabled)" when {

      "mandation status is in session" should {

        lazy val result = {
          mockConfig.features.submitReturnFeatures(true)
          callAuthService(individualAuthResult)
          callDateService()
          callOpenObligations(exampleObligations)
          callExtendedAudit
          callServiceInfoPartialService
          controller.returnDeadlines()(fakeRequest.withSession("mtdVatMandationStatus" -> "MTDfB Mandated"))
        }

        "return 200" in {
          status(result) shouldBe Status.OK
        }

        "return the regular return deadlines page" in {
          val document: Document = Jsoup.parse(bodyOf(result))
          document.getElementById("submit-return-link") shouldBe null
        }
      }

      "mandation status is not in session" should {

        lazy val result = {
          mockConfig.features.submitReturnFeatures(true)
          callAuthService(individualAuthResult)
          callDateService()
          callOpenObligations(exampleObligations)
          callExtendedAudit
          callServiceInfoPartialService
          callMandationService(Right(MandationStatus("MTDfB Mandated")))
          controller.returnDeadlines()(fakeRequest)
        }

        "return 200" in {
          status(result) shouldBe Status.OK
        }

        "return the regular return deadlines page" in {
          val document: Document = Jsoup.parse(bodyOf(result))
          document.getElementById("submit-return-link") shouldBe null
        }

        "put the mandation status in the session " in {
          session(result).get(SessionKeys.mtdVatMandationStatus) shouldBe Some("MTDfB Mandated")
        }
      }
    }

    "A user with no open obligations" should {

      lazy val result = {
        callAuthService(individualAuthResult)
        callDateService()
        callOpenObligations(emptyObligations)
        callFulfilledObligations(emptyObligations)
        callExtendedAudit
        callServiceInfoPartialService
        controller.returnDeadlines()(fakeRequest)
      }


      "return 200" in {
        status(result) shouldBe Status.OK
      }

      "return the no returns view" in {
        val document: Document = Jsoup.parse(bodyOf(result))
        document.select("article > p:nth-child(3)").text() shouldBe
          "You do not have any returns due right now. Your next deadline will show here on the first day of your next" +
            " accounting period."
      }
    }

    "the user is an agent (with agentAccess enabled)" when {

      lazy val result = {
        callAuthService(agentAuthResult)
        callAuthServiceEnrolmentsOnly(Enrolments(agentEnrolment))
        callDateService()
        callOpenObligations(exampleObligations)
        callExtendedAudit
        callMandationService(Right(MandationStatus("Non MTDfB")))
        controller.returnDeadlines()(fakeRequestWithClientsVRN)
      }

      "the client has open obligations" should {

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

      "the client has no open obligations" should {

        lazy val result = {
          callAuthService(agentAuthResult)
          callAuthServiceEnrolmentsOnly(Enrolments(agentEnrolment))
          callDateService()
          callOpenObligations(emptyObligations)
          callFulfilledObligations(emptyObligations)
          callExtendedAudit
          callMandationService(Right(MandationStatus("Non MTDfB")))
          controller.returnDeadlines()(fakeRequestWithClientsVRN)
        }

        "return 200" in {
          status(result) shouldBe Status.OK
        }

        "return the no returns view" in {
          val document: Document = Jsoup.parse(bodyOf(result))
          document.select("article > p:nth-child(3)").text() shouldBe
            "You do not have any returns due right now. Your next deadline will show here on the first day of your next" +
              " accounting period."
        }
      }

      "if the openObligations call fails" should {

        lazy val result = {
          callAuthService(agentAuthResult)
          callAuthServiceEnrolmentsOnly(Enrolments(agentEnrolment))
          callDateService()
          callOpenObligations(Left(ObligationError))
          callExtendedAudit
          callMandationService(Right(MandationStatus("Non MTDfB")))
          controller.returnDeadlines()(fakeRequestWithClientsVRN)
        }

        "return 500" in {
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "return the technical problem view" in {
          val document: Document = Jsoup.parse(bodyOf(result))
          document.title shouldBe "There is a problem with the service - VAT reporting through software - GOV.UK"
        }
      }
    }

    "A user is not authorised" should {

      lazy val result = {
        callAuthService(Future.failed(InsufficientEnrolments()))
        callDateService()
        controller.returnDeadlines()(fakeRequest)
      }

      "return 403 (Forbidden)" in {
        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "A user is not authenticated" should {

      lazy val result = {
        callAuthService(Future.failed(MissingBearerToken()))
        callDateService()
        controller.returnDeadlines()(fakeRequest)
      }

      "return 303 (SEE_OTHER)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to sign in" in {
        redirectLocation(result) shouldBe Some(mockConfig.signInUrl)
      }
    }

    "the Obligations API call fails" should {

      lazy val result = {
        callAuthService(individualAuthResult)
        callDateService()
        callOpenObligations(Left(ObligationError))
        controller.returnDeadlines()(fakeRequest)
      }

      "return 500" in {
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "return the technical problem view" in {
        val document: Document = Jsoup.parse(bodyOf(result))
        document.title shouldBe "There is a problem with the service - VAT reporting through software - GOV.UK"
      }
    }
  }

  "The .noUpcomingObligationsAction function" when {

    "the Returns service returns some fulfilled obligations" should {

      lazy val result = {
        (mockVatReturnService.getLastObligation(_: Seq[VatReturnObligation])).expects(*).returns(obligation)
        callFulfilledObligations(exampleObligations)
        controller.noUpcomingObligationsAction(Html(""), LocalDate.parse("2018-01-01"))(fakeRequest, user)
      }

      "return 200" in {
        status(result) shouldBe Status.OK
      }
    }

    "the Returns service returns an error" should {

      lazy val result = {
        callFulfilledObligations(Left(ObligationError))
        controller.noUpcomingObligationsAction(Html(""), LocalDate.parse("2018-01-01"))(fakeRequest, user)
      }

      "return 500" in {
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
