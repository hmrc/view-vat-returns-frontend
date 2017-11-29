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

package controllers

import javax.inject.{Inject, Singleton}

import config.AppConfig
import models.User
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import services.{EnrolmentsAuthService, VatReturnService}
import uk.gov.hmrc.auth.core.retrieve.Retrievals
import uk.gov.hmrc.auth.core.{AuthorisationException, Enrolment, NoActiveSession}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.Future

@Singleton
class VatReturnController @Inject()(val messagesApi: MessagesApi, enrolmentsAuthService: EnrolmentsAuthService,
                                    vatReturnService: VatReturnService, implicit val appConfig: AppConfig)
  extends FrontendController with I18nSupport {

  def yourVatReturn(): Action[AnyContent] = enrolledAction {
    implicit request =>
      implicit user =>
        vatReturnService.getVatReturn(user).map { vatReturn =>
          Ok(views.html.yourVatReturn(vatReturn))
        }
  }

  private def enrolledAction(block: Request[AnyContent] => User => Future[Result]): Action[AnyContent] = Action.async {
    implicit request =>
      enrolmentsAuthService.authorised(Enrolment("HMRC-MTD-VAT")).retrieve(Retrievals.authorisedEnrolments) {
        enrolments => {
          val user = User(enrolments)
          block(request)(user)
        }
      }.recover {
        case _: NoActiveSession => Redirect(controllers.routes.ErrorsController.sessionTimeout())
        case _: AuthorisationException => Redirect(controllers.routes.ErrorsController.unauthorised())
      }
  }
}
