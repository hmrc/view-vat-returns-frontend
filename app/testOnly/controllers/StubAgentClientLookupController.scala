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

package testOnly.controllers

import common.SessionKeys
import config.AppConfig

import javax.inject.Inject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import testOnly.forms.StubAgentClientLookupForm
import testOnly.views.html.{AgentClientLookupView, AgentClientUnauthView, AgentHubView}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

class StubAgentClientLookupController @Inject()(mcc: MessagesControllerComponents,
                                                agentClientLookupView: AgentClientLookupView,
                                                agentClientUnauthView: AgentClientUnauthView,
                                                agentHubView: AgentHubView)
                                               (implicit appConfig: AppConfig) extends FrontendController(mcc) {

  def show(redirectUrl: RedirectUrl): Action[AnyContent] = Action { implicit request =>
    Ok(agentClientLookupView(StubAgentClientLookupForm.form, redirectUrl.unsafeValue))
  }

  def unauth(redirectUrl: RedirectUrl): Action[AnyContent] = Action { implicit request =>
    Ok(agentClientUnauthView(redirectUrl.unsafeValue))
      .removingFromSession(SessionKeys.mtdVatvcClientVrn)
  }

  def agentHub: Action[AnyContent] = Action { implicit request =>
    Ok(agentHubView())
  }

  def post: Action[AnyContent] = Action { implicit request =>
    StubAgentClientLookupForm.form.bindFromRequest().fold(
      err => InternalServerError(s"Failed to bind model:\n\nError: $err"),
      success => Redirect(success.redirectUrl)
        .addingToSession(SessionKeys.mtdVatvcClientVrn -> success.vrn)
    )
  }
}
