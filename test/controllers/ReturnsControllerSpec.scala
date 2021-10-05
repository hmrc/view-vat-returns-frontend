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

import common.TestModels.customerInformationNonMTDfB
import models._
import models.errors.{NotFoundError, VatReturnError}
import models.payments.Payment
import models.viewModels.VatReturnViewModel
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.{Enrolments, InsufficientEnrolments, MissingBearerToken}
import views.html.errors.PreMtdReturnView
import views.html.returns.VatReturnDetailsView

import scala.concurrent.Future

class ReturnsControllerSpec extends ControllerBaseSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  def controller: ReturnsController = new ReturnsController(
    mcc,
    mockVatReturnService,
    mockSubscriptionService,
    mockDateService,
    mockServiceInfoService,
    mockAuthorisedController,
    inject[VatReturnDetailsView],
    errorHandler,
    inject[PreMtdReturnView]
  )

  def setupCommonSuccessMocks(): Any = {
    callObligationWithMatchingPeriodKey(Some(exampleObligation))
    callVatReturnPayment(Some(examplePayment))
    callSubscriptionService(Some(customerInformationNonMTDfB))
    callAudit
    callDateService()
  }

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
    1320.00,
    "#001"
  )

  val exampleObligation: VatReturnObligation = VatReturnObligation(
    LocalDate.parse("2017-01-01"),
    LocalDate.parse("2017-02-01"),
    LocalDate.parse("2017-02-02"),
    "F",
    Some(LocalDate.parse("2017-02-02")),
    "#001"
  )

  val exampleVatReturnDetails: VatReturnDetails =
    VatReturnDetails(exampleVatReturn, moneyOwed = true, oweHmrc = Some(true), Some(examplePayment))


  "Calling the .vatReturn action" when {

    "user is a non-agent and authorised" when {

      "mandation status is not in session" when {

        "a valid period key which isn't all numeric is provided" should {

          lazy val result = {
            callAuthService(individualAuthResult)
            callVatReturn(Right(exampleVatReturn))
            setupCommonSuccessMocks()
            callServiceInfoPartialService
            callConstructReturnDetailsModel(exampleVatReturnDetails)
            controller.vatReturn(2018, "#001")(request())
          }

          "return 200" in {
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
          }

          "return charset utf-8" in {
            charset(result) shouldBe Some("utf-8")
          }
        }

        "an invalid period key is provided" should {

          lazy val result = {
            callAuthService(individualAuthResult)
            controller.vatReturn(2018, "form-label")(request())
          }

          "return 404" in {
            status(result) shouldBe Status.NOT_FOUND
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
          }

          "return charset utf-8" in {
            charset(result) shouldBe Some("utf-8")
          }
        }

        "an all numeric period key is provided but a return cannot be found matching period key" should {

          lazy val result = {
            callAuthService(individualAuthResult)
            callVatReturn(Left(NotFoundError))
            setupCommonSuccessMocks()
            callServiceInfoPartialService
            controller.vatReturn(2018, "3002")(request())
          }

          "return 404" in {
            status(result) shouldBe Status.NOT_FOUND
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
          }

          "return charset utf-8" in {
            charset(result) shouldBe Some("utf-8")
          }

          "have the correct title" in {
            messages(Jsoup.parse(contentAsString(result)).select("h1").text) shouldBe "This return is not available"
          }
        }
      }
    }

    "user is an agent" when {

        "authorised" should {

          lazy val result = {
            callAuthService(agentAuthResult)
            callAuthServiceEnrolmentsOnly(Enrolments(agentEnrolment))
            callVatReturn(Right(exampleVatReturn))
            setupCommonSuccessMocks()
            callConstructReturnDetailsModel(exampleVatReturnDetails)
            controller.vatReturn(2018, "#001")(request(fakeRequestWithClientsVRN))
          }

          "return 200" in {
            status(result) shouldBe Status.OK
          }
        }

        "not authorised" should {

          "return 403" in {

            lazy val result = {
              callAuthService(agentAuthResult)
              callAuthServiceEnrolmentsOnly(Enrolments(mtdVatEnrolment))
              controller.vatReturn(2018, "#001")(request(fakeRequestWithClientsVRN))
            }

            status(result) shouldBe Status.FORBIDDEN
          }
        }

    }

    "a user is not authorised" should {

      lazy val result = {
        callAuthService(Future.failed(InsufficientEnrolments()))
        controller.vatReturn(2018, "#001")(request())
      }

      "return 403 (Forbidden)" in {
        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "a user is not authenticated" should {

      lazy val result = {
        callAuthService(Future.failed(MissingBearerToken()))
        controller.vatReturn(2018, "#001")(request())
      }

      "return 303 (SEE_OTHER)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to sign in" in {
        redirectLocation(result) shouldBe Some(mockConfig.signInUrl)
      }
    }

    "the specified VAT return is not found" should {

      "not redirect if year and periodKey are not in session" should {
        lazy val result = {
          callAuthService(individualAuthResult)
          callVatReturn(Left(NotFoundError))
          setupCommonSuccessMocks()
          callServiceInfoPartialService
          controller.vatReturn(2018, "#001")(request())
        }

        "return 404 (Not Found)" in {
          status(result) shouldBe Status.NOT_FOUND
        }
      }

      "redirect if the user has come from the submission confirmation page" should {
        lazy val result = {
          callAuthService(individualAuthResult)
          callVatReturn(Left(NotFoundError))
          setupCommonSuccessMocks()
          callServiceInfoPartialService
          controller.vatReturn(2018, "18AA")(
            request(fakeRequest.withSession(
              "submissionYear" -> "2018",
              "inSessionPeriodKey" -> "18AA"
            ))
          )
        }

        "return 303 (See Other)" in {
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some("/vat-through-software/vat-returns/submitted")
        }

        "submission year and period key will no longer be in session" in {
          session(result).get("submissionYear") shouldBe None
          session(result).get("inSessionPeriodKey") shouldBe None
        }
      }
    }

    "a different error is retrieved" should {

      lazy val result = {
        callAuthService(individualAuthResult)
        callVatReturn(Left(VatReturnError))
        setupCommonSuccessMocks()
        callServiceInfoPartialService
        controller.vatReturn(2018, "#001")(request())
      }

      "return 500 (Internal Server Error)" in {
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    insolvencyCheck(controller.vatReturn(2018, "#001"))
  }

  "Calling the .vatReturnViaPayments action" when {

    "user is a non-agent and authorised" when {

      "mandation status is not in session" when {

        "a valid period key is provided" should {

          lazy val result = {
            callAuthService(individualAuthResult)
            callVatReturn(Right(exampleVatReturn))
            setupCommonSuccessMocks()
            callServiceInfoPartialService
            callConstructReturnDetailsModel(exampleVatReturnDetails)
            controller.vatReturnViaPayments("#001")(request())
          }

          "return 200" in {
            status(result) shouldBe Status.OK
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
          }

          "return charset utf-8" in {
            charset(result) shouldBe Some("utf-8")
          }
        }

        "an invalid period key is provided" should {

          lazy val result = {
            callAuthService(individualAuthResult)
            controller.vatReturnViaPayments("form-label")(request())
          }

          "return 404" in {
            status(result) shouldBe Status.NOT_FOUND
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
          }

          "return charset utf-8" in {
            charset(result) shouldBe Some("utf-8")
          }
        }

        "an all numeric period key is provided but but a return cannot be found matching period key" should {

          lazy val result = {
            callAuthService(individualAuthResult)
            callVatReturn(Left(NotFoundError))
            setupCommonSuccessMocks()
            callServiceInfoPartialService
            controller.vatReturnViaPayments("3001")(request())
          }

          "return 404" in {
            status(result) shouldBe Status.NOT_FOUND
          }

          "return HTML" in {
            contentType(result) shouldBe Some("text/html")
          }

          "return charset utf-8" in {
            charset(result) shouldBe Some("utf-8")
          }

          "have the correct title" in {
            messages(Jsoup.parse(contentAsString(result)).select("h1").text) shouldBe "This return is not available"
          }
        }
      }

      "mandation status is in session" should {

        lazy val result = {
          callAuthService(individualAuthResult)
          callVatReturn(Right(exampleVatReturn))
          setupCommonSuccessMocks()
          callServiceInfoPartialService
          callConstructReturnDetailsModel(exampleVatReturnDetails)
          controller.vatReturnViaPayments("#001")(request(fakeRequest.withSession("mtdVatMandationStatus" -> "Non MTDfB")))
        }

        "not make a call to retrieve mandation status" in {
          status(result) shouldBe Status.OK
        }
      }
    }

    "user is an agent" when {

        "authorised" should {

          lazy val result = {
            callAuthService(agentAuthResult)
            callAuthServiceEnrolmentsOnly(Enrolments(agentEnrolment))
            callVatReturn(Right(exampleVatReturn))
            setupCommonSuccessMocks()
            callConstructReturnDetailsModel(exampleVatReturnDetails)
            controller.vatReturnViaPayments("#001")(request(fakeRequestWithClientsVRN))
          }

          "return 200" in {
            status(result) shouldBe Status.OK
          }
        }

        "not authorised" should {

          lazy val result = {
            callAuthService(agentAuthResult)
            callAuthServiceEnrolmentsOnly(Enrolments(mtdVatEnrolment))
            controller.vatReturnViaPayments("#001")(request(fakeRequestWithClientsVRN))
          }

          "return 403" in {
            status(result) shouldBe Status.FORBIDDEN
          }
        }

    }

    "a user is not authorised" should {

      lazy val result = {
        callAuthService(Future.failed(InsufficientEnrolments()))
        controller.vatReturnViaPayments("#001")(request(fakeRequestWithClientsVRN))
      }

      "return 403 (Forbidden)" in {
        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "a user is not authenticated" should {

      lazy val result = {
        callAuthService(Future.failed(MissingBearerToken()))
        controller.vatReturnViaPayments("#001")(request(fakeRequestWithClientsVRN))
      }

      "return 303 (SEE_OTHER)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to sign in" in {
        redirectLocation(result) shouldBe Some(mockConfig.signInUrl)
      }
    }

    "the specified VAT return is not found" should {

      lazy val result = {
        callAuthService(individualAuthResult)
        callVatReturn(Left(NotFoundError))
        setupCommonSuccessMocks()
        callServiceInfoPartialService
        controller.vatReturnViaPayments("#001")(request())
      }

      "return 404 (Not Found)" in {
        status(result) shouldBe Status.NOT_FOUND
      }
    }

    "a different error is retrieved" should {

      lazy val result = {
        callAuthService(individualAuthResult)
        callVatReturn(Left(VatReturnError))
        setupCommonSuccessMocks()
        callServiceInfoPartialService
        controller.vatReturn(2018, "#001")(request())
      }

      "return 500 (Internal Server Error)" in {
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    insolvencyCheck(controller.vatReturnViaPayments("#001"))
  }

  "Calling .constructViewModel" should {

    "populate a VatReturnViewModel" in {

      val expectedViewModel = VatReturnViewModel(
        entityName = Some("Cheapo Clothing"),
        periodFrom = exampleObligation.periodFrom,
        periodTo = exampleObligation.periodTo,
        dueDate = exampleObligation.due,
        returnTotal = examplePayment.outstandingAmount,
        dateSubmitted = exampleObligation.received.get,
        vatReturnDetails = exampleVatReturnDetails,
        showReturnsBreadcrumb = true,
        currentYear = 2018,
        hasFlatRateScheme = true,
        isHybridUser = false
      )

      (mockDateService.now: () => LocalDate).stubs().returns(LocalDate.parse("2018-05-01"))

      lazy val result: VatReturnViewModel = controller.constructViewModel(
        Some(customerInformationNonMTDfB),
        exampleObligation,
        exampleVatReturnDetails,
        isReturnsPageRequest = true
      )
      result shouldBe expectedViewModel
    }
  }

  "Calling .renderResult" when {

    "there is a VAT return, obligation and payment" when {

      val data = ReturnsControllerData(Right(exampleVatReturn), None, Some(examplePayment), Some(exampleObligation), Html(""))

      lazy val result = {
        callConstructReturnDetailsModel(exampleVatReturnDetails)
        callDateService()
        callAudit
        controller.renderResult(data, isReturnsPageRequest = true, isNumericPeriodKey = false)(request(), user)
      }

      "return an OK status" in {
        status(result) shouldBe Status.OK
      }
    }

    "the VAT return is not found" when {
      "the period key is not all numeric" should {

        "return a Not Found status" in {
          val data = ReturnsControllerData(Left(NotFoundError), None, None, None, Html(""))
          lazy val result = controller.renderResult(data, isReturnsPageRequest = true,
            isNumericPeriodKey = false)(request(), user)
          status(result) shouldBe Status.NOT_FOUND
        }
      }

      "the period key is all numeric" should {

        "return a Not Found status" in {
          val data = ReturnsControllerData(Left(NotFoundError), None, None, None, Html(""))
          lazy val result = controller.renderResult(data, isReturnsPageRequest = true,
            isNumericPeriodKey = true)(request(), user)
          status(result) shouldBe Status.NOT_FOUND
        }
      }
    }

    "there is a VAT return but no obligation" should {

      "return an Internal Server Error status" in {
        val data = ReturnsControllerData(Right(exampleVatReturn), None, None, None, Html(""))
        lazy val result = controller.renderResult(data, isReturnsPageRequest = true,
          isNumericPeriodKey = false)(request(), user)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "there is any other combination" should {

      "return an Internal Server Error status" in {
        val data = ReturnsControllerData(Left(VatReturnError), None, None, None, Html(""))
        lazy val result = controller.renderResult(data, isReturnsPageRequest = true,
          isNumericPeriodKey = false)(request(), user)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "Calling .validPeriodKey" when {

    "a valid alphanumeric-only period key is provided" should {

      "return true" in {
        controller.validPeriodKey("13AC") shouldBe true
      }
    }

    "a valid period key beginning with a # is provided" should {

      "return true" in {
        controller.validPeriodKey("#001") shouldBe true
      }
    }

    "a period key with lower case alphanumeric characters is provided" should {

      "return false" in {
        controller.validPeriodKey("13ac") shouldBe false
      }
    }

    "a period key with an unsupported character is provided" should {

      "return false" in {
        controller.validPeriodKey("13A*") shouldBe false
      }
    }

    "a period key with more than 4 characters is provided" should {

      "return false" in {
        controller.validPeriodKey("13ACL") shouldBe false
      }
    }

    "a period key with less than 4 characters is provided" should {

      "return false" in {
        controller.validPeriodKey("13A") shouldBe false
      }
    }
  }

  "Calling .numericPeriodKey" when {

    "a numeric period key is provided without a hash" should {

      "return true" in {
        controller.numericPeriodKey("1334") shouldBe true
      }
    }

    "a numeric period key is provided with a hash" should {

      "return false" in {
        controller.numericPeriodKey("#001") shouldBe false
      }
    }

    "an alphanumeric period key is provided" should {

      "return false" in {
        controller.numericPeriodKey("13aC") shouldBe false
      }
    }
  }
}
