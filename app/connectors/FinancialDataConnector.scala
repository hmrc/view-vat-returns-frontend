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

package connectors

import config.AppConfig
import connectors.httpParsers.ResponseHttpParsers.HttpGetResult
import javax.inject.{Inject, Singleton}
import models.payments.Payments
import play.api.Logger
import services.{DateService, MetricsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialDataConnector @Inject()(http: HttpClient,
                                       appConfig: AppConfig,
                                       metrics: MetricsService,
                                       dateService: DateService) {

  private[connectors] def paymentsUrl(vrn: String): String = s"${appConfig.financialDataBaseUrl}/financial-transactions/vat/$vrn"

  def getPayments(vrn: String, year: Option[Int])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpGetResult[Payments]] = {

    import connectors.httpParsers.PaymentsHttpParser.PaymentsReads

    val timer = metrics.getPaymentsTimer.time()

    val querySeq = year match {
      case Some(y) =>
        Seq(
          "dateFrom" -> s"$y-01-01",
          "dateTo" -> s"$y-12-31"
        )
      case _ =>
        Seq(
          "onlyOpenItems" -> "true"
        )
    }

    val httpRequest = http.GET(
      paymentsUrl(vrn),
      querySeq
    )

    httpRequest.map {
      case payments@Right(_) =>
        timer.stop()
        payments
      case httpError@Left(error) =>
        metrics.getPaymentsCallFailureCounter.inc()
        Logger.warn("FinancialDataConnector received error: " + error.message)
        httpError
    }
  }
}
