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

package controllers.predicate

import common._
import config.{AppConfig, ServiceErrorHandler}
import javax.inject.{Inject, Singleton}
import models.User
import play.api.mvc._
import services.{EnrolmentsAuthService, SubscriptionService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.allEnrolments
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggerUtil._
import views.html.errors.UnauthorisedView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthoriseAgentWithClient @Inject()(enrolmentsAuthService: EnrolmentsAuthService,
                                         subscriptionService: SubscriptionService,
                                         mcc: MessagesControllerComponents,
                                         unauthorisedView: UnauthorisedView,
                                         errorHandler: ServiceErrorHandler)
                                        (implicit appConfig: AppConfig,
                                         ec: ExecutionContext) extends FrontendController(mcc) {

  def authoriseAsAgent(block: MessagesRequest[AnyContent] => User => Future[Result], ignoreMandatedStatus: Boolean)
                      (implicit request: MessagesRequest[AnyContent]): Future[Result] = {

    val delegatedAuthRule: String => Enrolment = vrn =>
      Enrolment(EnrolmentKeys.mtdVatEnrolmentKey)
        .withIdentifier(EnrolmentKeys.vatIdentifierId, vrn)
        .withDelegatedAuthRule(EnrolmentKeys.mtdVatDelegatedAuthRule)

    request.session.get(SessionKeys.clientVrn) match {
      case Some(vrn) =>
        enrolmentsAuthService
          .authorised(delegatedAuthRule(vrn))
          .retrieve(allEnrolments) {
            enrolments =>
              enrolments.enrolments.collectFirst {
                case Enrolment(EnrolmentKeys.agentEnrolmentKey, EnrolmentIdentifier(_, arn) :: _, EnrolmentKeys.activated, _) => arn
              } match {
                case Some(arn) => checkMandationStatus(block, vrn, arn, ignoreMandatedStatus)
                case None =>
                  logDebug("[AuthPredicate][authoriseAsAgent] - Agent with no HMRC-AS-AGENT enrolment. Rendering unauthorised view.")
                  Future.successful(Forbidden(unauthorisedView()))
              }
          } recover {
          case _: NoActiveSession =>
            logDebug(s"AgentPredicate][authoriseAsAgent] - No active session. Redirecting to ${appConfig.signInUrl}")
            Redirect(appConfig.signInUrl)
          case _: AuthorisationException =>
            logDebug(s"[AgentPredicate][authoriseAsAgent] - Agent does not have delegated authority for Client. " +
              s"Redirecting to ${appConfig.agentClientUnauthorisedUrl(request.uri)}")
            Redirect(appConfig.agentClientUnauthorisedUrl(request.uri))
        }
      case None =>
        logDebug(s"[AuthoriseAsAgentWithClient][invokeBlock] - No Client VRN in session, redirecting to Select Client page")
        Future.successful(Redirect(appConfig.agentClientLookupUrl(request.uri)))
    }
  }


  private def checkMandationStatus(block: MessagesRequest[AnyContent] => User => Future[Result],
                                   vrn: String,
                                   arn: String,
                                   ignoreMandatedStatus: Boolean)
                                  (implicit request: MessagesRequest[AnyContent], hc: HeaderCarrier): Future[Result] = {

    val permittedStatuses: List[String] =
      List(MandationStatuses.nonMTDfB, MandationStatuses.nonDigital, MandationStatuses.mtdfbExempt)

    subscriptionService.getUserDetails(vrn) flatMap {
      case Some(details) if ignoreMandatedStatus || permittedStatuses.contains(details.mandationStatus) =>
        val user = User(vrn, arn = Some(arn))
        block(request)(user)
      case Some(_) =>
        logDebug("[AuthorisedAgentWithClient][checkMandationStatus] - Agent acting for MTDfB client")
        Future.successful(Redirect(appConfig.agentClientHubUrl))
      case None =>
        Future.successful(errorHandler.showInternalServerError)
    }
  }
}
