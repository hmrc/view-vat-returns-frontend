/*
 * Copyright 2023 HM Revenue & Customs
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

import akka.actor.ActorSystem
import common.EnrolmentKeys._
import common.SessionKeys
import common.SessionKeys.{mtdVatvcClientVrn, mtdVatvcSubmittedReturn}
import mocks.MockAuth
import models.User
import play.api.http.Status.FORBIDDEN
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys => GovUkSessionKeys}
import scala.concurrent.Future

class ControllerBaseSpec extends MockAuth {

  val vrn = "999999999"
  val arn = "XARN1234567"

  implicit val system: ActorSystem = ActorSystem()
  implicit val hc: HeaderCarrier = HeaderCarrier()

  def fakeRequestToPOSTWithSession(input: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] =
    fakeRequestWithSession.withFormUrlEncodedBody(input: _*)

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  def request(request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()): MessagesRequest[AnyContentAsEmpty.type] =
    new MessagesRequest[AnyContentAsEmpty.type](request.withSession(SessionKeys.insolventWithoutAccessKey -> "false",
      SessionKeys.futureInsolvencyDate -> "false"), mcc.messagesApi)
  implicit val user: User = User(vrn)

  lazy val fakeRequestWithSession: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
    GovUkSessionKeys.lastRequestTimestamp -> "1498236506662",
    GovUkSessionKeys.authToken -> "Bearer Token"
  )

  lazy val recentlySubmittedReturnRequest: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
    mtdVatvcSubmittedReturn -> "true"
  )

  lazy val insolventRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(SessionKeys.insolventWithoutAccessKey -> "true")

  lazy val insolventRequestError: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(SessionKeys.insolventWithoutAccessKey -> "false", SessionKeys.futureInsolvencyDate -> "true")

  lazy val insolventRequestTrade: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(SessionKeys.insolventWithoutAccessKey -> "false", SessionKeys.futureInsolvencyDate -> "false")

  lazy val insolventRequestNoTrade: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(SessionKeys.insolventWithoutAccessKey -> "true", SessionKeys.futureInsolvencyDate -> "false")

  lazy val mandationRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(SessionKeys.mtdVatMandationStatus -> "Non MTDfB")

  lazy val fakeRequestWithClientsVRN: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(mtdVatvcClientVrn -> vrn)

  val mtdVatEnrolment: Set[Enrolment] = Set(Enrolment(
    mtdVatEnrolmentKey,
    Seq(EnrolmentIdentifier(vatIdentifierId, vrn)),
    activated
  ))

  val vatDecEnrolment: Set[Enrolment] = Set(Enrolment(
    vatDecEnrolmentKey,
    Seq(EnrolmentIdentifier(vatDecIdentifierId, vrn)),
    activated
  ))

  val agentEnrolment: Set[Enrolment] = Set(Enrolment(
    agentEnrolmentKey,
    Seq(EnrolmentIdentifier(agentIdentifierId, arn)),
    activated,
    Some(mtdVatDelegatedAuthRule)
  ))

  val invalidIdentifierNameEnrolment: Set[Enrolment] =
    Set(Enrolment(mtdVatEnrolmentKey, Seq(EnrolmentIdentifier("F", vrn)), activated))

  val emptyIdentifiersEnrolment: Set[Enrolment] =
    Set(Enrolment(mtdVatEnrolmentKey, Seq(), activated))

  val individualAuthResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.successful(new ~(
    Enrolments(mtdVatEnrolment), Some(Individual)
  ))

  val noEnrolmentsAuthResponse: Future[~[Enrolments, Option[AffinityGroup]]] = Future.successful(new ~(
    Enrolments(Set()), Some(Individual)
  ))

  val noAffinityAuthResponse: Future[~[Enrolments, Option[AffinityGroup]]] = Future.successful(new ~(
    Enrolments(mtdVatEnrolment), None
  ))

  val migratedUserAuthResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.successful(new ~(
    Enrolments(mtdVatEnrolment ++ vatDecEnrolment), Some(Individual)
  ))

  val agentAuthResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.successful(new ~(
    Enrolments(agentEnrolment), Some(Agent)
  ))

  val invalidIdentifierNameAuthResult: Future[Enrolments ~ Some[AffinityGroup.Individual.type]] = Future.successful(new ~(
    Enrolments(invalidIdentifierNameEnrolment), Some(Individual)
  ))

  val emptyIdentifiersAuthResult: Future[Enrolments ~ Some[AffinityGroup.Individual.type]] = Future.successful(new ~(
    Enrolments(emptyIdentifiersEnrolment), Some(Individual)
  ))

  def insolvencyCheck(controllerAction: Action[AnyContent]): Unit = {

    "the user is insolvent and not continuing to trade" should {

      "return 403 (Forbidden)" in {
        val result = {
          callAuthService(individualAuthResult)
          controllerAction(insolventRequest)
        }
        status(result) shouldBe FORBIDDEN
      }
    }
  }
}
