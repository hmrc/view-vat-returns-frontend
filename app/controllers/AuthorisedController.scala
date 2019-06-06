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

package controllers

import common.EnrolmentKeys._
import config.AppConfig
import models.User
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.EnrolmentsAuthService
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthorisationException, Enrolment, NoActiveSession}
import uk.gov.hmrc.auth.core.retrieve.{Retrievals, ~}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import controllers.predicate.AuthoriseAgentWithClient
import javax.inject.Inject

import scala.concurrent.Future

class AuthorisedController @Inject()(enrolmentsAuthService: EnrolmentsAuthService,
                                     val messagesApi: MessagesApi,
                                     val agentWithClientPredicate: AuthoriseAgentWithClient,
                                     implicit val appConfig: AppConfig) extends FrontendController with I18nSupport {

  def authorisedAction(block: Request[AnyContent] => User => Future[Result], allowAgentAccess: Boolean = true): Action[AnyContent] = Action.async {
    implicit request =>

      val predicate =
        ((Enrolment(vatDecEnrolmentKey) or Enrolment(vatVarEnrolmentKey)) and Enrolment(mtdVatEnrolmentKey))
          .or(Enrolment(mtdVatEnrolmentKey))

      enrolmentsAuthService.authorised(predicate).retrieve(Retrievals.authorisedEnrolments and Retrievals.affinityGroup) {
        case _ ~ Some(AffinityGroup.Agent) if allowAgentAccess => agentWithClientPredicate.authoriseAsAgent(block)
        case enrolments ~ Some(_) => block(request)(User(enrolments, None))
      } recoverWith {
        case _: NoActiveSession => Future.successful(Unauthorized(views.html.errors.sessionTimeout()))
        case _: AuthorisationException => Future.successful(Forbidden(views.html.errors.unauthorised()))
      }
  }

}
