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

import common.SessionKeys
import common.TestModels.{customerInformationFutureInsolvent, customerInformationMax}
import play.api.mvc.{MessagesRequest, Request, Result}
import play.api.http.Status
import play.api.mvc.Results.Ok
import play.api.mvc.AnyContent
import scala.concurrent.Future
import play.api.test.Helpers._

class AuthorisedControllerSpec extends ControllerBaseSpec {

    def target(request: Request[AnyContent], ignoreMandatedStatus: Boolean = false): Future[Result] =
      mockAuthorisedController.authorisedAction({ _ => _ => Future.successful(Ok("welcome"))
      }, ignoreMandatedStatus)(new MessagesRequest[AnyContent](request, mcc.messagesApi))


    "AuthorisedController .insolvencySubscriptionCall" when {

      "User is insolvent and not continuing to trade" should {

        "return Forbidden (403)" in {

          lazy val result = {
            callAuthService(individualAuthResult)
            callSubscriptionService(Some(customerInformationMax.copy(isInsolvent = true, continueToTrade = Some(false))))
            callDateService()
            target(fakeRequest)
          }
          status(result) shouldBe Status.FORBIDDEN
        }
      }

      "User have a value in session for future insolvency and the value is true" should {

        "return ISE (500)" should {

          lazy val result = {
            callAuthService(individualAuthResult)
            callSubscriptionService(Some(customerInformationFutureInsolvent))
            callDateService()
            target(fakeRequest)
          }

          "return an internal server error" in {
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }

          "add both the insolvent and futureInsolvency flags to the session" in {
            session(result).get(SessionKeys.insolventWithoutAccessKey) shouldBe Some("false")
            session(result).get(SessionKeys.futureInsolvencyDate) shouldBe Some("true")
          }
        }
      }

      "User have a value in session for future insolvency and the value is false" should {

        "return OK (200)" should {

          lazy val result = {
            callAuthService(individualAuthResult)
            callSubscriptionService(Some(customerInformationFutureInsolvent.copy(insolvencyDate = Some("2018-05-01"))))
            callDateService()
            target(fakeRequest)
          }

          "return the correct status" in {
            status(result) shouldBe Status.OK
          }

          "return the correct content" in {
            contentAsString(result) shouldBe "welcome"
          }

          "add both the insolvent and futureInsolvency flags to the session" in {
            session(result).get(SessionKeys.insolventWithoutAccessKey) shouldBe Some("false")
            session(result).get(SessionKeys.futureInsolvencyDate) shouldBe Some("false")
          }
        }
      }

      "The customer info call fails" should {

        "return 500" in {
          lazy val result = {
            callAuthService(individualAuthResult)
            callSubscriptionService(None)
            callDateService()
            target(fakeRequest)
          }
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }
  }



