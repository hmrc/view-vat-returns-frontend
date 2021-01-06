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

package services

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import connectors.{FinancialDataConnector, VatObligationsConnector, VatReturnsConnector}
import models.Obligation.Status
import models.payments.{Payment, Payments}
import models._
import models.errors._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnsService @Inject()(vatObligationsConnector: VatObligationsConnector,
                               financialDataConnector: FinancialDataConnector,
                               vatReturnConnector: VatReturnsConnector) {

  def getVatReturn(vrn: String, periodKey: String)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[VatReturn]] =
    vatReturnConnector.getVatReturnDetails(vrn, periodKey).map {
      case Right(vatReturn) => Right(vatReturn)
      case Left(UnexpectedStatusError("404", _)) => Left(NotFoundError)
      case Left(_) => Left(VatReturnError)
    }

  def getReturnObligationsForYear(vrn: String,
                                  searchYear: Int,
                                  status: Status.Value)
                                 (implicit hc: HeaderCarrier,
                                  ec: ExecutionContext): Future[ServiceResponse[VatReturnObligations]] = {
    val from: LocalDate = LocalDate.parse(s"$searchYear-01-01")
    val to: LocalDate = LocalDate.parse(s"$searchYear-12-31")

    vatObligationsConnector.getVatReturnObligations(vrn, Some(from), Some(to), status).map {
      case Right(obligations) => Right(filterObligationsByDueDate(obligations, searchYear))
      case Left(_) => Left(ObligationError)
    }
  }

  def getOpenReturnObligations(vrn: String)
                              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[VatReturnObligations]] =
    vatObligationsConnector.getVatReturnObligations(vrn, status = Obligation.Status.Outstanding).map {
      case Right(obligations) => Right(obligations)
      case Left(_) => Left(ObligationError)
    }

  def getFulfilledObligations(vrn: String, currentDate: LocalDate)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[VatReturnObligations]] = {
    val from: LocalDate = currentDate.minusMonths(3)
    vatObligationsConnector.getVatReturnObligations(vrn, Some(from), Some(currentDate), Status.Fulfilled).map {
      case Right(obligations) => Right(obligations)
      case Left(_) => Left(ObligationError)
    }
  }

  def getLastObligation(obligations: Seq[VatReturnObligation]): VatReturnObligation = obligations.sortWith(_.due isAfter _.due).head

  def getObligationWithMatchingPeriodKey(vrn: String,
                                         year: Int,
                                         periodKey: String)
                                        (implicit hc: HeaderCarrier,
                                         ec: ExecutionContext): Future[Option[VatReturnObligation]] =
    getReturnObligationsForYear(vrn, year, Status.Fulfilled).map {
      case Right(VatReturnObligations(obligations)) => obligations.find(_.periodKey == periodKey)
      case Left(_) => None
    }

  private[services] def filterObligationsByDueDate(returnObligations: VatReturnObligations, searchYear: Int): VatReturnObligations =
    VatReturnObligations(returnObligations.obligations.filter(_.periodTo.getYear == searchYear))

  def getPayment(vrn: String, requiredPeriod: String, year: Option[Int] = None)
                (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Payment]] = {
    financialDataConnector.getPayments(vrn, year).map {
      case Right(payments) => filterPaymentsByPeriodKey(payments, requiredPeriod)
      case Left(_) => None
    }
  }

  private[services] def filterPaymentsByPeriodKey(payments: Payments, requiredPeriod: String): Option[Payment] =
    payments.financialTransactions.find(_.periodKey == requiredPeriod)

  def constructReturnDetailsModel(vatReturn: VatReturn, payment: Option[Payment]): VatReturnDetails = {
    val moneyOwed = payment.fold(true)(_.outstandingAmount != 0)
    val oweHmrc: Option[Boolean] = payment map {
      _.outstandingAmount > 0
    }
    VatReturnDetails(vatReturn, moneyOwed, oweHmrc, payment)
  }
}
