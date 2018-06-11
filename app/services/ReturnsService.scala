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
import connectors.{FinancialDataConnector, VatObligationsConnector, VatReturnsConnector}
import models.Obligation.Status
import models.payments.{Payment, Payments}
import models._
import models.errors.{NotFoundError, ObligationError, UnexpectedStatusError, VatReturnError}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnsService @Inject()(vatObligationsConnector: VatObligationsConnector, financialDataConnector: FinancialDataConnector,
                               vatReturnConnector: VatReturnsConnector) {

  def getVatReturn(user: User, periodKey: String)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[VatReturn]] =
    vatReturnConnector.getVatReturnDetails(user.vrn, periodKey).map {
      case Right(vatReturn) => Right(vatReturn)
      case Left(UnexpectedStatusError("404", _)) => Left(NotFoundError)
      case Left(_) => Left(VatReturnError)
    }

  def getReturnObligationsForYear(user: User, searchYear: Int, status: Status.Value)
                                 (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[VatReturnObligations]] = {
    val from: LocalDate = LocalDate.parse(s"$searchYear-01-01")
    val to: LocalDate = LocalDate.parse(s"$searchYear-12-31")

    vatObligationsConnector.getVatReturnObligations(user.vrn, from, to, status).map {
      case Right(obligations) =>
        Right(filterObligationsByDueDate(obligations, searchYear))
      case Left(_) => Left(ObligationError)
    }
  }

  def getFulfilledObligations(currentDate: LocalDate)
                             (implicit user: User, hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[VatReturnObligations]] = {
    val from: LocalDate = currentDate.minusMonths(3)
    vatObligationsConnector.getVatReturnObligations(user.vrn, from, currentDate, Status.Fulfilled).map {
      case Right(obligations) => Right(obligations)
      case Left(_) => Left(ObligationError)
    }
  }

  def getLastObligation(obligations: Seq[VatReturnObligation]): VatReturnObligation = obligations.sortWith(_.due isAfter _.due).head

  def getObligationWithMatchingPeriodKey(user: User, year: Int, periodKey: String)
                                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[VatReturnObligation]] =
    getReturnObligationsForYear(user, year, Status.Fulfilled).map {
      case Right(VatReturnObligations(obligations)) => obligations.find(_.periodKey == periodKey)
      case Left(_) => None
    }

  private[services] def filterObligationsByDueDate(returnObligations: VatReturnObligations, searchYear: Int): VatReturnObligations =
    VatReturnObligations(returnObligations.obligations.filter(_.end.getYear == searchYear))

  def getPayment(user: User, requiredPeriod: String)
                (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Payment]] =
    financialDataConnector.getPayments(user.vrn).map {
      case Right(payments) => filterPaymentsByPeriodKey(payments, requiredPeriod)
      case Left(_) => None
    }

  private[services] def filterPaymentsByPeriodKey(payments: Payments, requiredPeriod: String): Option[Payment] =
    payments.financialTransactions.find(_.periodKey == requiredPeriod)

  def constructReturnDetailsModel(vatReturn: VatReturn, payment: Option[Payment]): VatReturnDetails = {
    val moneyOwed = payment.fold(true)(_.outstandingAmount != 0)
    val isRepayment = vatReturn.vatReclaimedCurrentPeriod > vatReturn.totalVatDue
    VatReturnDetails(vatReturn, moneyOwed, isRepayment, payment)
  }
}
