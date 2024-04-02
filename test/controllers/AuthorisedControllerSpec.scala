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

package controllers

import common.SessionKeys
import common.TestModels.customerInformationFutureInsolvent
import org.jsoup.Jsoup
import play.api.mvc.{MessagesRequest, Request, Result}
import play.api.http.Status
import play.api.mvc.Results.Ok
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import scala.concurrent.Future
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{InsufficientEnrolments, InternalError}

class AuthorisedControllerSpec extends ControllerBaseSpec {

  def target(request: Request[AnyContent], ignoreMandatedStatus: Boolean = false): Future[Result] =
    mockAuthorisedController.authorisedAction({ _ => _ => Future.successful(Ok("welcome"))
    }, ignoreMandatedStatus)(new MessagesRequest[AnyContent](request, mcc.messagesApi))


  "AuthorisedController" when {

    "the user is an Individual (Principal Entity)" when {

      "they have an active HMRC-MTD-VAT enrolment" when {

        "they have a value in session for their insolvency status" when {

          "insolvent user not continuing to trade" should {

            lazy val result = {
              callAuthService(individualAuthResult)
              callDateService()
              target(insolventRequestNoTrade)
            }

            "return Forbidden (403)" in {
              status(result) shouldBe Status.FORBIDDEN
            }
          }

          "user is permitted to trade" should {

            lazy val result = {
              callAuthService(individualAuthResult)
              callDateService()
              target(insolventRequestTrade)
            }

            "return OK (200)" in {
              status(result) shouldBe Status.OK
            }

            "return the correct content" in {
              contentAsString(result) shouldBe "welcome"
            }
          }

          "user not permitted to trade" should {

            lazy val result = {
              callAuthService(individualAuthResult)
              callDateService()
              target(insolventRequestError)
            }

            "return an Internal Server Error (500)" in {
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }
        }

        "there is an error returned from customer information API" should {

          lazy val result = {
            callAuthService(individualAuthResult)
            callSubscriptionService(None)
            callDateService()
            target(fakeRequest)
          }

          "return 500" in {
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR

          }
        }
      }

      ".insolvencySubscriptionCall" should {

        "user not permitted to trade" should {

          lazy val result = {
            callAuthService(individualAuthResult)
            callSubscriptionService(Some(customerInformationFutureInsolvent))
            callDateService()
            target(fakeRequest)
          }

          "return an Internal Server Error (500)" in {
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }

          "adding sessionKeys" in {
            session(result).get(SessionKeys.insolventWithoutAccessKey) shouldBe Some("false")
            session(result).get(SessionKeys.futureInsolvencyDate) shouldBe Some("true")
          }
        }
      }

      "they do not have an active HMRC-MTD-VAT enrolment" should {

        lazy val result: Future[Result] = {
          callAuthService(Future.failed(InsufficientEnrolments()))
          target(fakeRequest)
        }

        "return Forbidden (403)" in {
          status(result) shouldBe Status.FORBIDDEN
        }

        "render the Not Signed Up page" in {
          Jsoup.parse(contentAsString(result)).title shouldBe "You are not authorised to use this service - VAT - GOV.UK"
        }
      }

      "they have no HMRC-MTD-VAT enrolment" should {

        lazy val result = {
          callAuthService(noEnrolmentsAuthResponse)
          target(FakeRequest())
        }

        "return Forbidden (403)" in {
          status(result) shouldBe Status.FORBIDDEN
        }

        "render the Not Signed Up page" in {
          Jsoup.parse(contentAsString(result)).title shouldBe "You are not authorised to use this service - VAT - GOV.UK"
        }
      }

      "they have an unrecognised identifier name" should {

        lazy val result = {
          callAuthService(invalidIdentifierNameAuthResult)
          target(FakeRequest())
        }

        "return Forbidden (403)" in {
          status(result) shouldBe Status.FORBIDDEN
        }
      }

      "they have no identifiers" should {

        lazy val result = {
          callAuthService(emptyIdentifiersAuthResult)
          target(FakeRequest())
        }

        "return Forbidden (403)" in {
          status(result) shouldBe Status.FORBIDDEN
        }
      }

      "there is a different authorisation exception" should {

        lazy val result: Future[Result] = {
          callAuthService(Future.failed(InternalError()))
          target(fakeRequest)
        }

        "return Forbidden (403)" in {
          status(result) shouldBe Status.FORBIDDEN
        }

        "render the Not Signed Up page" in {
          Jsoup.parse(contentAsString(result)).title shouldBe "You are not authorised to use this service - VAT - GOV.UK"
        }
      }
    }

    "the user has no affinity" should {

      lazy val result = {
        callAuthService(noAffinityAuthResponse)
        target(FakeRequest())
      }

      "return ISE (500)" in {
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "the user is agent and access is forbidden" should {

      lazy val result = {
        callAuthService(agentAuthResult)
        target(FakeRequest())
      }

      "redirect to agent hub page" in {

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(mockConfig.agentClientHubUrl)
      }

    }
  }
}



