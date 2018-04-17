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

import connectors.{FinancialDataConnector, PaymentsConnector}
import javax.inject.{Inject, Singleton}
import models.payments.{PaymentDetailsModel, Payments}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsService @Inject()(paymentsConnector: PaymentsConnector) {
  def setupPaymentsJourney(journeyDetails: PaymentDetailsModel)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] =
    paymentsConnector.setupJourney(journeyDetails).map {
      case Right(redirectUrl) => redirectUrl
      case Left(error) => throw new Exception(error.message)
    }

}
