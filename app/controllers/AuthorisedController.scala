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

import common.{SessionKeys, EnrolmentKeys => Keys}
import config.{AppConfig, ServiceErrorHandler}
import controllers.predicate.AuthoriseAgentWithClient

import javax.inject.{Inject, Singleton}
import models.User
import play.api.mvc._
import services.{DateService, EnrolmentsAuthService, SubscriptionService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.LoggerUtil
import views.html.errors.{InsolventUnauthView, UnauthorisedView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthorisedController @Inject()(enrolmentsAuthService: EnrolmentsAuthService,
                                     subscriptionService: SubscriptionService,
                                     agentWithClientPredicate: AuthoriseAgentWithClient,
                                     mcc: MessagesControllerComponents,
                                     unauthorisedView: UnauthorisedView,
                                     insolventUnauthView: InsolventUnauthView,
                                     errorHandler: ServiceErrorHandler,
                                     dateService: DateService)
                                    (implicit appConfig: AppConfig,
                                     ec: ExecutionContext) extends FrontendController(mcc)  with LoggerUtil {

  def authorisedAction(block: MessagesRequest[AnyContent] => User => Future[Result],
                       allowAgentAccess: Boolean = true): Action[AnyContent] = Action.async { implicit request =>

    enrolmentsAuthService.authorised.retrieve(Retrievals.allEnrolments and Retrievals.affinityGroup) {
      case _ ~ Some(AffinityGroup.Agent) =>
        if (allowAgentAccess) {
          agentWithClientPredicate.authoriseAsAgent(block)
        } else {
          logger.debug("[AuthorisedController][authorisedAction] User is agent and agent access is forbidden. Redirecting to Agent Action page.")
          Future.successful(Redirect(appConfig.agentClientHubUrl))
        }
      case enrolments ~ Some(_) => authorisedAsNonAgent(block, enrolments)
      case _ =>
        logger.warn("[AuthorisedController][authorisedAction] - Missing affinity group")
        Future.successful(errorHandler.showInternalServerError)
    } recoverWith {
      case _: NoActiveSession => Future.successful(Redirect(appConfig.signInUrl))
      case _: InsufficientEnrolments =>
        logger.warn(s"[AuthorisedController][authorisedAction] insufficient enrolment exception encountered")
        Future.successful(Forbidden(unauthorisedView()))
      case _: AuthorisationException =>
        logger.warn(s"[AuthorisedController][authorisedAction] encountered unauthorisation exception")
        Future.successful(Forbidden(unauthorisedView()))
    }
  }

  private def authorisedAsNonAgent(block: MessagesRequest[AnyContent] => User => Future[Result], enrolments: Enrolments)
                                  (implicit request: MessagesRequest[AnyContent]): Future[Result] = {

    val vatEnrolments: Set[Enrolment] = User.extractVatEnrolments(enrolments)

    if (vatEnrolments.exists(_.key == Keys.mtdVatEnrolmentKey)) {
      val containsNonMtdVat: Boolean = User.containsNonMtdVat(vatEnrolments)

      vatEnrolments.collectFirst {
        case Enrolment(Keys.mtdVatEnrolmentKey, Seq(EnrolmentIdentifier(Keys.vatIdentifierId, vrn)), status, _) =>

          val user = User(vrn, status == Keys.activated, containsNonMtdVat)

          (request.session.get(SessionKeys.insolventWithoutAccessKey), request.session.get(SessionKeys.futureInsolvencyDate)) match {
            case (Some("true"), _) => Future.successful(Forbidden(insolventUnauthView(user)))
            case (Some("false"), Some("true")) => Future.successful(errorHandler.showInternalServerError)
            case (Some("false"), Some("false")) => block(request)(user)
            case _ => insolvencySubscriptionCall(user, block(request))
          }

      } getOrElse {
        logger.warn("[AuthorisedController][authoriseAsNonAgent] Non-agent with invalid VRN")
        Future.successful(errorHandler.showInternalServerError)
      }
    } else {
      logger.debug("[AuthorisedController][authoriseAsNonAgent] Non-agent with no HMRC-MTD-VAT enrolment. Rendering unauthorised view.")
      Future.successful(Forbidden(unauthorisedView()))
    }
  }

  private[controllers] def insolvencySubscriptionCall(user: User, block: User => Future[Result])
                                                     (implicit request: MessagesRequest[AnyContent]): Future[Result] =

    subscriptionService.getUserDetails(user.vrn).flatMap {
      case Some(details) =>
        (details.isInsolventWithoutAccess, details.insolvencyDateFutureUserBlocked(dateService.now())) match {
          case (true, futureDateBlock) =>
            logger.debug("[AuthorisedController][insolvencySubscriptionCall] - User is insolvent and not continuing to trade")
            Future.successful(
              Forbidden(insolventUnauthView(user)).addingToSession(
                SessionKeys.insolventWithoutAccessKey -> "true",
                SessionKeys.futureInsolvencyDate -> s"$futureDateBlock")
            )
          case (_, true) =>
            logger.debug("[AuthorisedController][insolvencySubscriptionCall] - User has a future insolvencyDate, throwing ISE")
            Future.successful(
              errorHandler.showInternalServerError.addingToSession(
                SessionKeys.insolventWithoutAccessKey -> "false",
                SessionKeys.futureInsolvencyDate -> "true")
            )
          case _ =>
            logger.debug("[AuthorisedController][insolvencySubscriptionCall] - Authenticated as principle")
            block(user).map(result => result.addingToSession(
              SessionKeys.insolventWithoutAccessKey -> "false",
              SessionKeys.futureInsolvencyDate -> "false")
            )
        }
      case _ =>
        logger.warn("[AuthorisedController][insolvencySubscriptionCall] - Failure obtaining insolvency status from Customer Info API")
        Future.successful(errorHandler.showInternalServerError)
    }
}
