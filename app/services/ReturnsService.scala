/*
 * Copyright 2019 HM Revenue & Customs
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
import connectors.{FinancialDataConnector, VatObligationsConnector, VatReturnsConnector, VatSubscriptionConnector}
import models.Obligation.Status
import models.payments.{Payment, Payments}
import models._
import models.errors._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnsService @Inject()(vatObligationsConnector: VatObligationsConnector, financialDataConnector: FinancialDataConnector,
                               vatReturnConnector: VatReturnsConnector, vatSubscriptionConnector: VatSubscriptionConnector) {

  def getVatReturn(user: User, periodKey: String)
                  (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[VatReturn]] =
    vatReturnConnector.getVatReturnDetails(user.vrn, periodKey).map {
      case Right(vatReturn) => Right(vatReturn)
      case Left(UnexpectedStatusError("404", _)) => Left(NotFoundError)
      case Left(_) => Left(VatReturnError)
    }

  def getReturnObligationsForYear(user: User,
                                  searchYear: Int,
                                  status: Status.Value)
                                 (implicit hc: HeaderCarrier,
                                  ec: ExecutionContext,
                                  migrationDate: Option[String]): Future[ServiceResponse[VatReturnObligations]] = {
    val from: LocalDate = LocalDate.parse(s"$searchYear-01-01")
    val to: LocalDate = LocalDate.parse(s"$searchYear-12-31")

    vatObligationsConnector.getVatReturnObligations(user.vrn, Some(from), Some(to), status).flatMap {
      case Right(obligations) if status == Status.Fulfilled =>
        filterPreETMPObligations(obligations, migrationDate, user).map {
          case Some(filteredObligations) => Right(filterObligationsByDueDate(filteredObligations, searchYear))
          case _ => Left(VatSubscriptionError)
        }
      case Right(obligations) => Future.successful(Right(filterObligationsByDueDate(obligations, searchYear)))
      case Left(_) => Future.successful(Left(ObligationError))
    }
  }

  def filterPreETMPObligations(obligations: VatReturnObligations, migrationDate: Option[String], user: User)
                              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[VatReturnObligations]] =
    migrationDate match {
      case Some(date) if date.nonEmpty =>
        Future.successful(Some(
          VatReturnObligations(obligations.obligations.filterNot(_.start.isBefore(LocalDate.parse(date))))
        ))
      case _ =>
        vatSubscriptionConnector.getCustomerInfo(user.vrn).map {
          case Right(customerInfo) =>
            customerInfo.customerMigratedToETMPDate match {
              case Some(date) =>
                Some(VatReturnObligations(obligations.obligations.filterNot(_.start.isBefore(LocalDate.parse(date)))))
              case _ =>
                None
            }
          case Left(_) => None
        }
    }

  def getOpenReturnObligations(user: User)
                              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[VatReturnObligations]] =
    vatObligationsConnector.getVatReturnObligations(user.vrn, status = Obligation.Status.Outstanding).map {
      case Right(obligations) => Right(obligations)
      case Left(_) => Left(ObligationError)
    }

  def getFulfilledObligations(currentDate: LocalDate)
                             (implicit user: User, hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[VatReturnObligations]] = {
    val from: LocalDate = currentDate.minusMonths(3)
    vatObligationsConnector.getVatReturnObligations(user.vrn, Some(from), Some(currentDate), Status.Fulfilled).map {
      case Right(obligations) => Right(obligations)
      case Left(_) => Left(ObligationError)
    }
  }

  def getLastObligation(obligations: Seq[VatReturnObligation]): VatReturnObligation = obligations.sortWith(_.due isAfter _.due).head

  def getObligationWithMatchingPeriodKey(user: User,
                                         year: Int,
                                         periodKey: String)
                                        (implicit hc: HeaderCarrier,
                                         ec: ExecutionContext,
                                         migrationDate: Option[String]): Future[Option[VatReturnObligation]] =
    getReturnObligationsForYear(user, year, Status.Fulfilled).map {
      case Right(VatReturnObligations(obligations)) => obligations.find(_.periodKey == periodKey)
      case Left(_) => None
    }

  private[services] def filterObligationsByDueDate(returnObligations: VatReturnObligations, searchYear: Int): VatReturnObligations =
    VatReturnObligations(returnObligations.obligations.filter(_.end.getYear == searchYear))

  def getPayment(user: User, requiredPeriod: String, year: Option[Int] = None)
                (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Payment]] = {
    financialDataConnector.getPayments(user.vrn, year).map {
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

  def getMandationStatus(vrn: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ServiceResponse[MandationStatus]] = {
    vatSubscriptionConnector.getMandationStatusInfo(vrn) map {
      case Right(manStatus) => Right(manStatus)
      case Left(_) => Left(MandationStatusError)
    }
  }
}
