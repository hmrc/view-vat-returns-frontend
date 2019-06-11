/*
 * Copyright 2019 HM Revenue & Customs
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
import models.{MandationStatus, User}
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.{EnrolmentsAuthService, ReturnsService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.allEnrolments
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class AuthoriseAgentWithClient @Inject()(enrolmentsAuthService: EnrolmentsAuthService,
                                         mandationStatusService: ReturnsService,
                                         val messagesApi: MessagesApi,
                                         implicit val appConfig: AppConfig
                                        ) extends FrontendController with I18nSupport {

  def authoriseAsAgent(block: Request[AnyContent] => User => Future[Result])
                      (implicit request: Request[AnyContent]): Future[Result] = {

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
                case Some(arn) => checkMandationStatus(block, vrn, arn)
                case None =>
                  //$COVERAGE-OFF$ Disabling scoverage for Logger
                  Logger.debug("[AuthPredicate][authoriseAsAgent] - Agent with no HMRC-AS-AGENT enrolment. Rendering unauthorised view.")
                  //$COVERAGE-ON$ Disabling scoverage for Logger
                  Future.successful(Forbidden(views.html.errors.unauthorised()))
              }
          } recover {
          case _: NoActiveSession =>
            Logger.debug(s"AgentPredicate][authoriseAsAgent] - No active session. Redirecting to ${appConfig.signInUrl}")
            Redirect(appConfig.signInUrl)
          case _: AuthorisationException =>
            //$COVERAGE-OFF$ Disabling scoverage for Logger
            Logger.debug(s"[AgentPredicate][authoriseAsAgent] - Agent does not have delegated authority for Client. " +
              s"Redirecting to ${appConfig.agentClientUnauthorisedUrl(request.uri)}")
            //$COVERAGE-ON$
            Redirect(appConfig.agentClientUnauthorisedUrl(request.uri))
        }
      case None =>
        //$COVERAGE-OFF$ Disabling scoverage for Logger
        Logger.debug(s"[AuthoriseAsAgentWithClient][invokeBlock] - No Client VRN in session, redirecting to Select Client page")
        //$COVERAGE-ON$ Disabling scoverage for Logger
        Future.successful(Redirect(appConfig.agentClientLookupUrl(request.uri)))
    }
  }


  private def checkMandationStatus(block: Request[AnyContent] => User => Future[Result], vrn: String, arn: String)
                                  (implicit request: Request[AnyContent], hc: HeaderCarrier): Future[Result] = {

    mandationStatusService.getMandationStatus(vrn) flatMap {
      case Right(MandationStatus(MandationStatuses.nonMTDfB)) =>
        val user = User(vrn, arn = Some(arn))
        block(request)(user)
      case Right(_) =>
        //$COVERAGE-OFF$ Disabling scoverage for Logger
        Logger.debug("[AuthPredicate][checkMandationStatus] - Agent acting for MTDfB client")
        //$COVERAGE-ON$ Disabling scoverage for Logger
        Future.successful(Redirect(appConfig.agentClientActionUrl))
      case Left(error) =>
        //$COVERAGE-OFF$ Disabling scoverage for Logger
        Logger.warn(s"[AuthPredicate][checkMandationStatus] - Error returned from mandationStatusService: $error")
        //$COVERAGE-ON$ Disabling scoverage for Logger
        Future.successful(InternalServerError)
    }
  }
}