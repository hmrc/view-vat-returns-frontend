/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject
import config.AppConfig
import connectors.httpParsers.ResponseHttpParsers.HttpGetResult
import models.viewModels.{ReturnDeadlineViewModel, ReturnObligationsViewModel, VatReturnsViewModel}
import models.{Obligation, User, VatReturnObligations}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.{DateService, EnrolmentsAuthService, ReturnsService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ReturnObligationsController @Inject()(val messagesApi: MessagesApi,
                                            val enrolmentsAuthService: EnrolmentsAuthService,
                                            returnsService: ReturnsService,
                                            dateService: DateService,
                                            implicit val appConfig: AppConfig)
  extends AuthorisedController {

  def submittedReturns(year: Int): Action[AnyContent] = authorisedAction { implicit request =>
    implicit user =>
      if(isValidSearchYear(year)) {
        getReturnObligations(user, year, Obligation.Status.Fulfilled).map {
          case Right(model) => Ok(views.html.returns.submittedReturns(model))
          case Left(_) => InternalServerError(views.html.errors.submittedReturnsError(user))
        }
      } else {
        Future.successful(NotFound(views.html.errors.notFound()))
      }
  }

  def returnDeadlines(): Action[AnyContent] = authorisedAction { implicit request =>
    implicit user =>
      returnsService.getReturnObligationsForYear(user, dateService.now().getYear, Obligation.Status.Outstanding).map {
        case Right(VatReturnObligations(obligations)) =>
          val deadlines = obligations.map(ob =>
            ReturnDeadlineViewModel(ob.due, ob.start, ob.end, ob.due.isBefore(dateService.now())))
          Ok(views.html.returns.returnDeadlines(deadlines, user.vrn))
        case Left(_) => InternalServerError(views.html.errors.technicalProblem())
      }
  }

  private[controllers] def isValidSearchYear(year: Int, upperBound: Int = dateService.now().getYear) = {
    year <= upperBound && year >= upperBound - 1
  }

  private[controllers] def getReturnObligations(user: User, selectedYear: Int, status: Obligation.Status.Value)
                                               (implicit hc: HeaderCarrier): Future[HttpGetResult[VatReturnsViewModel]] = {

    val returnYears: Seq[Int] = Seq[Int](2018)

    returnsService.getReturnObligationsForYear(user, selectedYear, status).map {
      case Right(VatReturnObligations(obligations)) => Right(VatReturnsViewModel(
        returnYears,
        selectedYear,
        obligations.map( obligation =>
          ReturnObligationsViewModel(
            obligation.start,
            obligation.end,
            obligation.periodKey
          )
        ),
        user.hasNonMtdVat,
        user.vrn
      ))
      case Left(error)=> Left(error)
    }
  }
}
