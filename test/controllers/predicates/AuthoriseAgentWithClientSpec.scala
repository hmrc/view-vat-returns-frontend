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

package controllers.predicates

import common.TestModels._
import controllers.ControllerBaseSpec
import play.api.http.Status
import play.api.mvc.{AnyContent, MessagesRequest, Request, Result}
import play.api.mvc.Results.Ok
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.Helpers._

import scala.concurrent.{ExecutionContext, Future}

class AuthoriseAgentWithClientSpec extends ControllerBaseSpec {

    lazy val authResponse: Enrolments = Enrolments(agentEnrolment)

    def target(request: Request[AnyContent], ignoreMandatedStatus: Boolean = false): Future[Result] =
      mockAuthorisedAgentWithClient.authoriseAsAgent({ _ =>
        _ => Ok("welcome")
      }, ignoreMandatedStatus)(new MessagesRequest[AnyContent](request, mcc.messagesApi))

  "AgentPredicate .authoriseAsAgent" when {

    "CLIENT_VRN is in session" when {

      "agent has delegated enrolment for VRN" when {

        "agent has HMRC-AS-AGENT enrolment" when {

          "client is Non MTDfB" should {

            "return the result of the original code block" in {

              callAuthServiceEnrolmentsOnly(authResponse)
              callSubscriptionService(Some(customerInformationNonMTDfB))

              lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

              status(result) shouldBe Status.OK
              await(bodyOf(result)) shouldBe "welcome"
            }
          }

          "client is Non Digital" should {

            "return the result of the original code block" in {

              callAuthServiceEnrolmentsOnly(authResponse)
              callSubscriptionService(Some(customerInformationNonDigital))

              lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

              status(result) shouldBe Status.OK
              await(bodyOf(result)) shouldBe "welcome"
            }
          }

          "client is MTDfB Exempt" should {

            "return the result of the original code block" in {

              callAuthServiceEnrolmentsOnly(authResponse)
              callSubscriptionService(Some(customerInformationMTDfBExempt))

              lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

              status(result) shouldBe Status.OK
              await(bodyOf(result)) shouldBe "welcome"
            }
          }

          "agent does not have HMRC-AS-AGENT enrolment" should {

            "return 403" in {

              val otherEnrolment: Enrolments = Enrolments(
                Set(
                  Enrolment(
                    "OTHER-ENROLMENT",
                    Seq(EnrolmentIdentifier("AA", "AA")),
                    "Activated"
                  )
                )
              )

              callAuthServiceEnrolmentsOnly(Future.successful(otherEnrolment))

              lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

              status(result) shouldBe Status.FORBIDDEN
            }
          }
        }

        "agent does not have delegated enrolment for VRN" should {

          "redirect to agent-client-lookup unauthorised page" in {

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(Future.failed(InsufficientEnrolments()))

          lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

            await(status(result)) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(mockConfig.agentClientUnauthorisedUrl("/"))
          }
        }

        "client does not have the correct mandation status" should {

          "redirect to agent-client-lookup agent access page" in {

            callAuthServiceEnrolmentsOnly(authResponse)
            callSubscriptionService(Some(customerInformationMax))

            lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

            await(status(result)) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(mockConfig.agentClientHubUrl)
          }

          "correctly redirect if 'ignoreMandatedStatus' is set to true" in {

            callAuthServiceEnrolmentsOnly(authResponse)
            callSubscriptionService(Some(customerInformationMax))

            lazy val result: Future[Result] =
              target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"), ignoreMandatedStatus = true)

            await(status(result)) shouldBe Status.OK
            await(bodyOf(result)) shouldBe "welcome"
          }
        }

        "an error is returned from the customer info call" should {

          "throw an internal server error" in {

            callAuthServiceEnrolmentsOnly(authResponse)
            callSubscriptionService(None)

            lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

            await(status(result)) shouldBe Status.INTERNAL_SERVER_ERROR
          }

        }

        "user has no session" should {

          "redirect to sign in" in {

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(Future.failed(MissingBearerToken()))

            lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(mockConfig.signInUrl)
          }
        }
      }

      "CLIENT_VRN is not in session" should {

        "redirect to agent-client lookup VRN lookup page" in {

          lazy val result: Future[Result] = target(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(mockConfig.agentClientLookupUrl("/"))
        }
      }
    }
  }
}
