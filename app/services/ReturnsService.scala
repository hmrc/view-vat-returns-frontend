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

package services

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import connectors.httpParsers.ResponseHttpParsers.HttpGetResult
import connectors.VatApiConnector
import models.VatReturnObligation.Status
import models.{User, VatReturn, VatReturnObligations}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class ReturnsService @Inject()(connector: VatApiConnector) {

  def getVatReturnDetails(user: User, start: LocalDate, end: LocalDate)
                         (implicit hc: HeaderCarrier): Future[HttpGetResult[VatReturn]] = {
    connector.getVatReturnDetails(user.vrn, start, end)
  }

  def getReturnObligationsForYear(user: User, searchYear: Int)
                                 (implicit hc: HeaderCarrier): Future[HttpGetResult[VatReturnObligations]] = {
    val from: LocalDate = LocalDate.parse(s"$searchYear-01-01")
    val to: LocalDate = LocalDate.parse(s"$searchYear-12-31")

    connector.getVatReturnObligations(user.vrn, from, to, Status.All).map {
      case Right(obligations) => Right(filterObligationsByDueDate(obligations, searchYear))
      case error@Left(_) => error
    }
  }

  private[services] def filterObligationsByDueDate(obligations: VatReturnObligations, searchYear: Int): VatReturnObligations = {
    VatReturnObligations(
      obligations.obligations.filter(_.end.getYear == searchYear)
    )
  }
}
