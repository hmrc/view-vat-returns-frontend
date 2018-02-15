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
import connectors.{FinancialDataConnector, VatApiConnector}
import models.VatReturnObligation.Status
import models.payments.{Payment, Payments}
import models.{User, VatReturn, VatReturnObligation, VatReturnObligations}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnsService @Inject()(vatApiConnector: VatApiConnector, financialDataConnector: FinancialDataConnector) {

  def getVatReturnDetails(user: User, periodKey: String)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpGetResult[VatReturn]] = {
    vatApiConnector.getVatReturnDetails(user.vrn, periodKey)
  }

  def getReturnObligationsForYear(user: User, searchYear: Int, status: Status.Value)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpGetResult[VatReturnObligations]] = {
    val from: LocalDate = LocalDate.parse(s"$searchYear-01-01")
    val to: LocalDate = LocalDate.parse(s"$searchYear-12-31")

    vatApiConnector.getVatReturnObligations(user.vrn, from, to, status).map {
      case Right(obligations) => Right(filterObligationsByDueDate(obligations, searchYear))
      case error@Left(_) => error
    }
  }

  def getObligationWithMatchingPeriodKey(user: User, year: Int, periodKey: String)
                                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[VatReturnObligation]] = {
    getReturnObligationsForYear(user, year, Status.Fulfilled).map {
      case Right(obs) => obs.obligations.find(_.periodKey == periodKey)
      case Left(_) => None
    }
  }

  private[services] def filterObligationsByDueDate(obligations: VatReturnObligations, searchYear: Int): VatReturnObligations = {
    VatReturnObligations(
      obligations.obligations.filter(_.end.getYear == searchYear)
    )
  }

  def getPayment(user: User, requiredPeriod: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Payment]] = {
    financialDataConnector.getPayments(user.vrn).map {
      case Right(payments) => filterPaymentsByPeriodKey(payments, requiredPeriod)
      case Left(_) => None
    }
  }

  private[services] def filterPaymentsByPeriodKey(payments: Payments, requiredPeriod: String): Option[Payment] = {
    payments.financialTransactions.find(_.periodKey == requiredPeriod)
  }

}
