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

package controllers.auth

import AuthPredicate.AuthPredicate
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import services.EnrolmentsAuthService
import uk.gov.hmrc.auth.core.NoActiveSession
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait AuthenticatedFrontendController extends FrontendController {

  def authService: EnrolmentsAuthService

  type ActionBody = Request[AnyContent] => User => Result
  type AsyncActionBody = Request[AnyContent] => User => Future[Result]

  type AuthenticatedAction = AsyncActionBody => Action[AnyContent]

  protected object AuthAction {

    def apply(actionBody: ActionBody): Action[AnyContent] = async(actionBody andThen (_ andThen Future.successful))

    private def internal(predicate: AuthPredicate)(action: AsyncActionBody): Action[AnyContent] = {
      Action.async { implicit request =>
        authService.authorised().retrieve(allEnrolments) { enrolments =>
          val user = User(enrolments)

          AuthPredicates.predicates.apply(request)(user) match {
            case Right(AuthPredicate.Success) => action(request)(user)
            case Left(failureResult) => failureResult
          }
        }.recover {
          case _: NoActiveSession => Redirect(controllers.routes.ErrorsController.sessionTimeout())
        }
      }
    }

    def async: AuthenticatedAction = internal(AuthPredicates.predicates)

  }

}
