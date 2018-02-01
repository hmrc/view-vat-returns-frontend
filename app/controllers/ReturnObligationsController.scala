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

import java.time.LocalDate
import javax.inject.Inject

import config.AppConfig
import models.viewModels.ReturnDeadline
import models.{User, VatReturnObligations}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.{EnrolmentsAuthService, ReturnsService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ReturnObligationsController @Inject()(val messagesApi: MessagesApi,
                                            val enrolmentsAuthService: EnrolmentsAuthService,
                                            returnsService: ReturnsService,
                                            implicit val appConfig: AppConfig)
  extends AuthorisedController {

  def completedReturns(year: Int): Action[AnyContent] = authorisedAction { implicit request =>
    implicit user =>
      if(validateSearchYear(year)) {
        getReturnObligations(user, year).map { returnObligations =>
          Ok(views.html.returns.completedReturns(returnObligations, Seq(2018, 2017, 2016, 2015), 2016))
        }
      } else {
        Future.successful(NotFound(views.html.errors.notFound()))
      }
  }

  def returnDeadlines(): Action[AnyContent] = authorisedAction { implicit request =>
    implicit user =>
      Future.successful(Ok(views.html.returns.returnDeadlines(
        ReturnDeadline(LocalDate.parse("2018-08-07"),
          LocalDate.parse("2017-08-01"),
          LocalDate.parse("2017-10-31"))
      )))
  }

  private[controllers] def validateSearchYear(year: Int, upperBound: Int = LocalDate.now().getYear) = {
    year <= upperBound && year >= upperBound - 3
  }

  private[controllers] def getReturnObligations(user: User, year: Int)(implicit hc: HeaderCarrier): Future[VatReturnObligations] = {
    returnsService.getReturnObligationsForYear(user, year).map {
      case Right(vatReturnObligations) => vatReturnObligations
      case Left(_) => VatReturnObligations(Seq())
    }
  }
}
