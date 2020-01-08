/*
 * Copyright 2020 HM Revenue & Customs
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
import controllers.predicate.AuthoriseAgentWithClient
import models.errors.MandationStatusError
import models.{MandationStatus, ServiceResponse}
import play.api.http.Status
import play.api.mvc.{AnyContent, Request, Result}
import play.api.mvc.Results.Ok
import services.{EnrolmentsAuthService, ReturnsService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.Helpers._

import scala.concurrent.{ExecutionContext, Future}

class AuthoriseAgentWithClientSpec extends ControllerBaseSpec {

  private trait Test {
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)
    val mockReturnsService: ReturnsService = mock[ReturnsService]
    val mandationStatusResult: String => Future[ServiceResponse[MandationStatus]]
    = mandationStatus => Future.successful(Right(MandationStatus(mandationStatus)))

    lazy val authResponse = Enrolments(
      Set(
        Enrolment(
          "HMRC-AS-AGENT",
          Seq(EnrolmentIdentifier("AgentReferenceNumber", "XARN1234567")),
          "Activated"
        )
      )
    )

    lazy val mockAgentPredicate: AuthoriseAgentWithClient = new AuthoriseAgentWithClient(
      mockEnrolmentsAuthService,
      mockReturnsService,
      messages,
      mockConfig
    )

    def target(request: Request[AnyContent], ignoreMandatedStatus: Boolean = false): Future[Result] = mockAgentPredicate.authoriseAsAgent({
      implicit request =>
        implicit user =>
          Ok("welcome")
    }, ignoreMandatedStatus)(request)
  }

  "AgentPredicate .authoriseAsAgent" when {

    "CLIENT_VRN is in session" when {

      "agent has delegated enrolment for VRN" when {

        "agent has HMRC-AS-AGENT enrolment" when {

          "client is Non MTDfB" should {

            "return the result of the original code block" in new Test {

              (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
                .expects(*, *, *, *)
                .returns(authResponse)

              (mockReturnsService.getMandationStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
                .expects(*, *, *)
                .returns(mandationStatusResult("Non MTDfB"))

              lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

              status(result) shouldBe Status.OK
              await(bodyOf(result)) shouldBe "welcome"
            }
          }

          "client is Non Digital" should {

            "return the result of the original code block" in new Test {

              (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
                .expects(*, *, *, *)
                .returns(authResponse)

              (mockReturnsService.getMandationStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
                .expects(*, *, *)
                .returns(mandationStatusResult("Non Digital"))

              lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

              status(result) shouldBe Status.OK
              await(bodyOf(result)) shouldBe "welcome"
            }
          }
        }

        "agent does not have HMRC-AS-AGENT enrolment" should {

          "return 403" in new Test {

            val otherEnrolment = Enrolments(
              Set(
                Enrolment(
                  "OTHER-ENROLMENT",
                  Seq(EnrolmentIdentifier("AA", "AA")),
                  "Activated"
                )
              )
            )

            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *, *)
              .returns(Future.successful(otherEnrolment))

            lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

            status(result) shouldBe Status.FORBIDDEN
          }
        }
      }

      "agent does not have delegated enrolment for VRN" should {

        "redirect to agent-client-lookup unauthorised page" in new Test {

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(Future.failed(InsufficientEnrolments()))

          lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

          await(status(result)) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(mockConfig.agentClientUnauthorisedUrl("/"))
        }
      }

      "client does not have the correct mandation status" should {

        "redirect to agent-client-lookup agent access page" in new Test {

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(authResponse)

          (mockReturnsService.getMandationStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *)
            .returns(mandationStatusResult("MTDfB Mandated"))

          lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

          await(status(result)) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(mockConfig.agentClientActionUrl)
        }

        "correctly redirect if 'ignoreMandatedStatus' is set to true" in new Test {

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(authResponse)

          (mockReturnsService.getMandationStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *)
            .returns(mandationStatusResult("MTDfB Mandated"))

          lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"), ignoreMandatedStatus = true)

          await(status(result)) shouldBe Status.OK
          await(bodyOf(result)) shouldBe "welcome"
        }

        "redirect to agent-client-lookup agent access page if 'ignoreMandatedStatus' is true, but mandation status is incorrect" in new Test {

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(authResponse)

          (mockReturnsService.getMandationStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *)
            .returns(mandationStatusResult("MTDfB Different Mandated Status"))

          lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

          await(status(result)) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(mockConfig.agentClientActionUrl)
        }

      }

      "an error is returned from the mandation status call" should {

        "throw an internal server error" in new Test {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(authResponse)

          (mockReturnsService.getMandationStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *)
            .returns(Left(MandationStatusError))

          lazy val result: Future[Result] = target(fakeRequest.withSession("CLIENT_VRN" -> "123456789"))

          await(status(result)) shouldBe Status.INTERNAL_SERVER_ERROR
        }

      }

      "user has no session" should {

        "redirect to sign in" in new Test {

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

      "redirect to agent-client lookup VRN lookup page" in new Test {

        lazy val result: Future[Result] = target(fakeRequest)

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(mockConfig.agentClientLookupUrl("/"))
      }
    }
  }
}
