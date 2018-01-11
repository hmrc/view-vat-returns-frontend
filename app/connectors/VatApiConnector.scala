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

package connectors

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import config.AppConfig
import connectors.httpParsers.VatReturnObligationsHttpParser._
import models.VatReturnObligation.Status
import models.{VatReturn, VatReturnObligations}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatApiConnector @Inject()(http: HttpClient, appConfig: AppConfig) {

  private[connectors] def obligationsUrl(vrn: String): String = s"${appConfig.vatApiBaseUrl}/vat/$vrn/obligations"

  // TODO: Replace with a real call to an endpoint once it becomes available. This returns static data for now.
  def getTradingName(vrn: String): Future[String] = {
    Future.successful("Cheapo Clothing Ltd")
  }

  // TODO: Replace with a real call to an endpoint once it becomes available. This returns static data for now.
  def getVatReturnDetails(vrn: String, periodKey: String): Future[VatReturn] = {
    Future.successful(
      VatReturn(
        LocalDate.parse("2017-01-01"),
        LocalDate.parse("2017-03-31"),
        LocalDate.parse("2017-04-06"),
        LocalDate.parse("2017-04-08"),
        1297,
        5755,
        7052,
        5732,
        1320,
        77656,
        765765,
        55454,
        545645
      )
    )
  }

  def getVatReturnObligations(vrn: String, from: LocalDate, to: LocalDate, status: Status.Value)
                          (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpGetResult[VatReturnObligations]] = {
    http.GET(obligationsUrl(vrn), Seq("from" -> from.toString, "to" -> to.toString, "status" -> status.toString)).map {
      case vatReturns@Right(_) => vatReturns
      case httpError@Left(error) =>
        Logger.info("VatApiConnector received error: " + error.message)
        httpError
    }
  }
}
