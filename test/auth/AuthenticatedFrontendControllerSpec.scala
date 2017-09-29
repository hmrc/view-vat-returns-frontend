/*
 * Copyright 2017 HM Revenue & Customs
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

package auth

import config.AppConfig
import org.scalamock.scalatest.MockFactory
import play.api.i18n.MessagesApi
import play.api.inject.Injector
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import services.AuthService
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedFrontendControllerSpec extends UnitSpec with MockFactory with WithFakeApplication {

  private trait Test extends AuthenticatedFrontendController {
    lazy val injector: Injector = fakeApplication.injector
    lazy val messages: MessagesApi = injector.instanceOf[MessagesApi]
    lazy val mockConfig: AppConfig = injector.instanceOf[AppConfig]

    val mockAuthConnector = mock[AuthConnector]
    val mockAuthService = new AuthService(mockAuthConnector)

    implicit val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    override val authService = mockAuthService
  }

  "AuthAction" should {

    "execute the action body if the user is authenticated" in new Test {

      val action = AuthAction {
        implicit req => user => Ok
      }

      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(Future.successful(
          Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("", "12345")), "", ConfidenceLevel.L100, None))))
        )

      val result = action(fakeRequest)

      status(result) shouldBe 200
    }

    "redirect and not execute the action body if the user is not authenticated" in new Test {
      val action = AuthAction {
        implicit req => user => Ok
      }

      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(Future.failed(new BearerTokenExpired))

      val result = action(fakeRequest)

      status(result) shouldBe 303
    }

  }

  "AuthAction.async" should {

    "execute the action body if the user is authenticated" in new Test {

      val action = AuthAction.async {
        implicit req => user => Future.successful(Ok)
      }

      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(Future.successful(
          Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("", "12345")), "", ConfidenceLevel.L100, None))))
        )

      val result = action(fakeRequest)

      status(result) shouldBe 200
    }

    "redirect and not execute the action body if the user is not authenticated" in new Test {
      val action = AuthAction {
        implicit req => user => Ok
      }

      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(Future.failed(new BearerTokenExpired))

      val result = action(fakeRequest)

      status(result) shouldBe 303
    }

  }

}
