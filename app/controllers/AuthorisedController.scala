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

package controllers

import common.{EnrolmentKeys => Keys}
import config.AppConfig
import models.User
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, MessagesRequest, Result}
import services.EnrolmentsAuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import controllers.predicate.AuthoriseAgentWithClient
import javax.inject.{Inject, Singleton}
import utils.LoggerUtil._
import views.html.errors.UnauthorisedView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthorisedController @Inject()(enrolmentsAuthService: EnrolmentsAuthService,
                                     agentWithClientPredicate: AuthoriseAgentWithClient,
                                     mcc: MessagesControllerComponents,
                                     unauthorisedView: UnauthorisedView)
                                    (implicit appConfig: AppConfig,
                                     ec: ExecutionContext) extends FrontendController(mcc) {

  def authorisedAction(block: MessagesRequest[AnyContent] => User => Future[Result],
                       allowAgentAccess: Boolean = true,
                       ignoreMandatedStatus: Boolean = false): Action[AnyContent] = Action.async { implicit request =>

      enrolmentsAuthService.authorised.retrieve(Retrievals.allEnrolments and Retrievals.affinityGroup) {
        case _ ~ Some(AffinityGroup.Agent) =>
          if (allowAgentAccess && appConfig.features.agentAccess()) {
            agentWithClientPredicate.authoriseAsAgent(block, ignoreMandatedStatus)
          } else {
            logDebug("[AuthorisedController][authorisedAction] User is agent and agent access is forbidden. Redirecting to Agent Action page.")
            Future.successful(Redirect(appConfig.agentClientActionUrl))
          }
        case enrolments ~ Some(_) => authorisedAsNonAgent(block, enrolments)
        case _ =>
          logWarn("[AuthorisedController][authorisedAction] - Missing affinity group")
          Future.successful(InternalServerError)
      } recoverWith {
        case _: NoActiveSession => Future.successful(Redirect(appConfig.signInUrl))
        case _: InsufficientEnrolments =>
          logWarn(s"[AuthorisedController][authorisedAction] insufficient enrolment exception encountered")
          Future.successful(Forbidden(unauthorisedView()))
        case _: AuthorisationException =>
          logWarn(s"[AuthorisedController][authorisedAction] encountered unauthorisation exception")
          Future.successful(Forbidden(unauthorisedView()))
      }
  }

  private def authorisedAsNonAgent(block: MessagesRequest[AnyContent] => User => Future[Result], enrolments: Enrolments)
                                  (implicit request: MessagesRequest[AnyContent]): Future[Result] = {

    val vatEnrolments: Set[Enrolment] = User.extractVatEnrolments(enrolments)

    if (vatEnrolments.exists(_.key == Keys.mtdVatEnrolmentKey)) {
      val containsNonMtdVat: Boolean = User.containsNonMtdVat(vatEnrolments)

      vatEnrolments.collectFirst {
        case Enrolment(Keys.mtdVatEnrolmentKey, EnrolmentIdentifier(Keys.vatIdentifierId, vrn) :: _, status, _) =>

          val user = User(vrn, status == Keys.activated, containsNonMtdVat)

          block(request)(user)

      } getOrElse {
        logWarn("[AuthPredicate][authoriseAsNonAgent] Non-agent with invalid VRN")
        Future.successful(InternalServerError)
      }
    } else {
      logDebug("[AuthPredicate][authoriseAsNonAgent] Non-agent with no HMRC-MTD-VAT enrolment. Rendering unauthorised view.")
      Future.successful(Forbidden(unauthorisedView()))
    }
  }
}
