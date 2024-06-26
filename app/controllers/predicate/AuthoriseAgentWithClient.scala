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

package controllers.predicate

import common._
import config.AppConfig
import javax.inject.{Inject, Singleton}
import models.User
import play.api.mvc._
import services.EnrolmentsAuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.LoggerUtil
import views.html.errors.UnauthorisedView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthoriseAgentWithClient @Inject()(enrolmentsAuthService: EnrolmentsAuthService,
                                         mcc: MessagesControllerComponents,
                                         unauthorisedView: UnauthorisedView)
                                        (implicit appConfig: AppConfig,
                                         ec: ExecutionContext) extends FrontendController(mcc) with LoggerUtil {

  def authoriseAsAgent(block: MessagesRequest[AnyContent] => User => Future[Result])
                      (implicit request: MessagesRequest[AnyContent]): Future[Result] = {

    val delegatedAuthRule: String => Enrolment = vrn =>
      Enrolment(EnrolmentKeys.mtdVatEnrolmentKey)
        .withIdentifier(EnrolmentKeys.vatIdentifierId, vrn)
        .withDelegatedAuthRule(EnrolmentKeys.mtdVatDelegatedAuthRule)

    request.session.get(SessionKeys.mtdVatvcClientVrn) match {
      case Some(vrn) =>
        enrolmentsAuthService
          .authorised(delegatedAuthRule(vrn))
          .retrieve(allEnrolments) {
            enrolments =>
              enrolments.enrolments.collectFirst {
                case Enrolment(EnrolmentKeys.agentEnrolmentKey, Seq(EnrolmentIdentifier(_, arn)), EnrolmentKeys.activated, _) => arn
              } match {
                case Some(arn) =>
                  val user = User(vrn, arn = Some(arn))
                  block(request)(user)
                case None =>
                  logger.debug("[AuthPredicate][authoriseAsAgent] - Agent with no HMRC-AS-AGENT enrolment. Rendering unauthorised view.")
                  Future.successful(Forbidden(unauthorisedView()))
              }
          } recover {
          case _: NoActiveSession =>
            logger.debug(s"AgentPredicate][authoriseAsAgent] - No active session. Redirecting to ${appConfig.signInUrl}")
            Redirect(appConfig.signInUrl)
          case _: AuthorisationException =>
            logger.debug(s"[AgentPredicate][authoriseAsAgent] - Agent does not have delegated authority for Client. " +
              s"Redirecting to ${appConfig.agentClientUnauthorisedUrl(request.uri)}")
            Redirect(appConfig.agentClientUnauthorisedUrl(request.uri))
        }
      case None =>
        logger.debug(s"[AuthoriseAsAgentWithClient][invokeBlock] - No Client VRN in session, redirecting to Select Client page")
        Future.successful(Redirect(appConfig.agentClientLookupUrl(request.uri)))
    }
  }

}
