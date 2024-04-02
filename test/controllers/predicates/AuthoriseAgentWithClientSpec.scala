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

package controllers.predicates

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

    def target(request: Request[AnyContent]): Future[Result] =
      mockAuthorisedAgentWithClient.authoriseAsAgent({ _ =>
        _ => Future.successful(Ok("welcome"))
      })(new MessagesRequest[AnyContent](request, mcc.messagesApi))

  "AgentPredicate .authoriseAsAgent" when {

    "mtdVatvcClientVrn is in session" when {

      "agent has delegated enrolment for VRN" when {

        "agent has HMRC-AS-AGENT enrolment" when {

          "return the result of the original code block" in {

            callAuthServiceEnrolmentsOnly(authResponse)

            lazy val result: Future[Result] = target(fakeRequest.withSession("mtdVatvcClientVrn" -> "123456789"))

            status(result) shouldBe Status.OK
            contentAsString(result) shouldBe "welcome"
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

            callAuthServiceEnrolmentsOnly(otherEnrolment)

            lazy val result: Future[Result] = target(fakeRequest.withSession("mtdVatvcClientVrn" -> "123456789"))

            status(result) shouldBe Status.FORBIDDEN
          }
        }


        "agent does not have delegated enrolment for VRN" should {

          "redirect to agent-client-lookup unauthorised page" in {

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(Future.failed(InsufficientEnrolments()))

          lazy val result: Future[Result] = target(fakeRequest.withSession("mtdVatvcClientVrn" -> "123456789"))

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(mockConfig.agentClientUnauthorisedUrl("/"))
          }
        }


        "user has no session" should {

          "redirect to sign in" in {

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(Future.failed(MissingBearerToken()))

            lazy val result: Future[Result] = target(fakeRequest.withSession("mtdVatvcClientVrn" -> "123456789"))

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(mockConfig.signInUrl)
          }
        }
      }

      "mtdVatvcClientVrn is not in session" should {

        "redirect to agent-client lookup VRN lookup page" in {

          lazy val result: Future[Result] = target(fakeRequest)

          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(mockConfig.agentClientLookupUrl("/"))
        }
      }
    }
  }
}
