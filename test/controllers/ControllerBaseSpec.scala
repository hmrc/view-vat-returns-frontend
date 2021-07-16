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

import akka.actor.ActorSystem
import common.EnrolmentKeys._
import common.SessionKeys
import common.SessionKeys.clientVrn
import controllers.predicate.DDInterruptPredicate
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
  val ddInterruptPredicate: DDInterruptPredicate = new DDInterruptPredicate(
    mcc
  )

  def fakeRequestToPOSTWithSession(input: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] =
    fakeRequestWithSession.withFormUrlEncodedBody(input: _*)

  lazy val DDInterruptRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET","/homepage")

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(SessionKeys.viewedDDInterrupt -> "true")

  def request(request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()): MessagesRequest[AnyContentAsEmpty.type] =
    new MessagesRequest[AnyContentAsEmpty.type](request.withSession(SessionKeys.insolventWithoutAccessKey -> "false",
      SessionKeys.futureInsolvencyDate -> "false", SessionKeys.viewedDDInterrupt -> "true"), mcc.messagesApi)
  implicit val user: User = User(vrn)

  lazy val fakeRequestWithSession: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
    GovUkSessionKeys.lastRequestTimestamp -> "1498236506662",
    GovUkSessionKeys.authToken -> "Bearer Token"
  )
  lazy val insolventRequest: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(SessionKeys.insolventWithoutAccessKey -> "true")

  lazy val fakeRequestWithClientsVRN: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(clientVrn -> vrn)

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

  val individualAuthResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.successful(new ~(
    Enrolments(mtdVatEnrolment), Some(Individual)
  ))

  val migratedUserAuthResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.successful(new ~(
    Enrolments(mtdVatEnrolment ++ vatDecEnrolment), Some(Individual)
  ))

  val agentAuthResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.successful(new ~(
    Enrolments(agentEnrolment), Some(Agent)
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
