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

import models._
import models.errors.ObligationError
import models.viewModels.{ReturnObligationsViewModel, VatReturnsViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._

import scala.concurrent.Future

class SubmittedReturnsControllerSpec extends ControllerBaseSpec {

  val exampleObligations: Future[ServiceResponse[VatReturnObligations]] = Right(
    VatReturnObligations(
      Seq(
        VatReturnObligation(
          LocalDate.parse("2222-01-01"),
          LocalDate.parse("2222-12-31"),
          LocalDate.parse("2222-01-31"),
          "O",
          None,
          "#001"
        )
      )
    )
  )
  val emptyObligations: ServiceResponse[VatReturnObligations] = Right(VatReturnObligations(Seq.empty))
  val previousYear: Int = 2017
  implicit val migrationDate: Option[String] = None

  def controller: SubmittedReturnsController = new SubmittedReturnsController(
    messages,
    enrolmentsAuthService,
    mockVatReturnService,
    mockAuthorisedController,
    mockDateService,
    mockServiceInfoService,
    mockSubscriptionService,
    mockConfig,
    mockAuditService
  )

  "Calling the .submittedReturns action" when {

    "A user is logged in and enrolled to HMRC-MTD-VAT" should {

      lazy val result = {
        callDateService()
        callExtendedAudit
        callAuthService(individualAuthResult)
        callOpenObligationsForYear(exampleObligations)
        callOpenObligationsForYear(exampleObligations)
        callServiceInfoPartialService
        controller.submittedReturns(previousYear)(fakeRequest)
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

    "an agent logs into the service" when {

      "agent access is enabled" when {

        "there is a clients vrn in session" when {

          "the agent has the correct authorised delegation" should {

            lazy val result = {
              callDateService()
              callExtendedAudit
              callAuthService(agentAuthResult)
              callAuthServiceEnrolmentsOnly(Enrolments(agentEnrolment))
              callMandationService(Right(MandationStatus("Non MTDfB")))
              callOpenObligationsForYear(exampleObligations)
              callOpenObligationsForYear(exampleObligations)
              controller.submittedReturns(previousYear)(fakeRequestWithClientsVRN)
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
      }

      "agent access is disabled" should {

        lazy val result = {
          mockConfig.features.agentAccess(false)
          callAuthService(agentAuthResult)
          controller.submittedReturns(previousYear)(fakeRequestWithClientsVRN)
        }

        "return 403 (Forbidden)" in {
          status(result) shouldBe Status.FORBIDDEN
        }

        "render the unauthorised page" in {
          val document: Document = Jsoup.parse(bodyOf(result))
          document.title shouldBe "You are not authorised to use this service"
        }
      }
    }

    "A user is not authorised" should {

      lazy val result = {
        callAuthService(Future.failed(InsufficientEnrolments()))
        controller.submittedReturns(previousYear)(fakeRequest)
      }

      "return 403 (Forbidden)" in {
        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "A user is not authenticated" should {

      lazy val result = {
        callAuthService(Future.failed(MissingBearerToken()))
        controller.submittedReturns(previousYear)(fakeRequest)
      }

      "return 401 (Unauthorised)" in {
        status(result) shouldBe Status.UNAUTHORIZED
      }
    }

    "A user enters an invalid search year for their submitted returns" should {

      lazy val result = {
        callAuthService(individualAuthResult)
        callDateService()
        controller.submittedReturns(year = 2021)(fakeRequest)
      }

      "return 404 (Not Found)" in {
        status(result) shouldBe Status.NOT_FOUND
      }
    }

    "An error occurs upstream" should {

      lazy val result: Result = {
        callAuthService(individualAuthResult)
        callDateService()
        callServiceInfoPartialService
        callOpenObligationsForYear(Left(ObligationError))
        controller.submittedReturns(previousYear)(fakeRequest)
      }

      "return 500" in {
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "return the submitted returns error view" in {
        val document: Document = Jsoup.parse(bodyOf(result))
        document.select("h1").first().text() shouldBe "Sorry, there is a problem with the service"
      }
    }
  }

  "Calling the .getReturnObligations function" when {

    "the vatReturnsService retrieves a valid list of VatReturnObligations" when {

      "the mocked date is 2020 or above" should {

        "return a VatReturnsViewModel with valid obligations" in {

          lazy val result = {
            callDateService(LocalDate.parse("2020-05-05"))
            callOpenObligationsForYear(exampleObligations)
            callOpenObligationsForYear(exampleObligations)
            callExtendedAudit
            controller.getReturnObligations(user, 2020, Obligation.Status.All)
          }

          val expectedResult = Right(VatReturnsViewModel(
            Seq(2020, 2019, 2018),
            2020,
            Seq(ReturnObligationsViewModel(
              LocalDate.parse("2222-01-01"),
              LocalDate.parse("2222-12-31"),
              "#001"
            )),
            hasNonMtdVatEnrolment = false,
            vrn
          ))

          await(result) shouldBe expectedResult
        }

        "return a VatReturnsViewModel with empty obligations" in {

          lazy val result = {
            callDateService(LocalDate.parse("2020-05-05"))
            callOpenObligationsForYear(emptyObligations)
            callOpenObligationsForYear(emptyObligations)
            callOpenObligationsForYear(emptyObligations)
            callExtendedAudit
            controller.getReturnObligations(user, 2020, Obligation.Status.All)
          }

          val expectedResult = Right(VatReturnsViewModel(
            Seq(2020),
            2020,
            List(),
            hasNonMtdVatEnrolment = false,
            vrn
          ))

          await(result) shouldBe expectedResult
        }
      }

      "the mocked date is below 2020" should {

        "return a VatReturnsViewModel with valid obligations" in {

          lazy val result = {
            callDateService()
            callOpenObligationsForYear(exampleObligations)
            callOpenObligationsForYear(exampleObligations)
            callExtendedAudit
            controller.getReturnObligations(user, 2017, Obligation.Status.All)
          }

          val expectedResult = Right(VatReturnsViewModel(
            Seq(2018, 2017),
            2017,
            Seq(
              ReturnObligationsViewModel(
                LocalDate.parse("2222-01-01"),
                LocalDate.parse("2222-12-31"),
                "#001"
              )
            ),
            hasNonMtdVatEnrolment = false,
            vrn
          ))

          await(result) shouldBe expectedResult
        }

        "return a VatReturnsViewModel with empty obligations" in {

          lazy val result = {
            callDateService()
            callOpenObligationsForYear(emptyObligations)
            callExtendedAudit
            controller.getReturnObligations(user, 2017, Obligation.Status.All)
          }

          val expectedResult = Right(VatReturnsViewModel(
            Seq(2018),
            2017,
            Seq(),
            hasNonMtdVatEnrolment = false,
            vrn
          ))

          await(result) shouldBe expectedResult
        }
      }
    }

    "the vatReturnsService retrieves an error" should {

      "return None" in {

        lazy val result = {
          callDateService()
          callOpenObligationsForYear(Left(ObligationError))
          await(controller.getReturnObligations(user, 2017, Obligation.Status.All))
        }

        result shouldBe Left(ObligationError)
      }
    }
  }

  "Calling .isValidSearchYear" when {

    "the year is on the upper search boundary" should {

      "return true" in {
        val result: Boolean = controller.isValidSearchYear(2018, 2018)
        result shouldBe true
      }
    }

    "the year is above the upper search boundary" should {

      "return false" in {
        val result: Boolean = controller.isValidSearchYear(2019, 2018)
        result shouldBe false
      }
    }

    "the year is on the lower boundary" should {

      "return true" in {
        val result: Boolean = controller.isValidSearchYear(2017, 2019)
        result shouldBe true
      }
    }

    "the year is below the lower boundary" should {

      "return false" in {
        val result: Boolean = controller.isValidSearchYear(2014, 2018)
        result shouldBe false
      }
    }

    "the year is between the upper and lower boundaries" should {

      "return true" in {
        val result: Boolean = controller.isValidSearchYear(2017, 2018)
        result shouldBe true
      }
    }
  }
}
