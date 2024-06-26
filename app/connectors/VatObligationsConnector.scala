/*
 * Copyright 2024 HM Revenue & Customs
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
import config.AppConfig
import connectors.httpParsers.ResponseHttpParsers.HttpResult
import javax.inject.{Inject, Singleton}
import models.Obligation.Status
import models.VatReturnObligations
import play.api.http.HeaderNames
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import utils.LoggerUtil
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatObligationsConnector @Inject()(http: HttpClient,
                                        appConfig: AppConfig) extends LoggerUtil {


  private[connectors] def obligationsUrl(vrn: String): String = {
    val baseUrl: String = appConfig.vatObligationsBaseUrl + "/vat-obligations"
    s"$baseUrl/$vrn/obligations"
  }

  private def headerCarrier(hc: HeaderCarrier) = hc.withExtraHeaders(
    HeaderNames.ACCEPT -> "application/vnd.hmrc.1.0+json",
    HeaderNames.CONTENT_TYPE -> "application/json"
  )

  def getVatReturnObligations(vrn: String, from: Option[LocalDate] = None, to: Option[LocalDate] = None, status: Status.Value)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResult[VatReturnObligations]] = {

    import connectors.httpParsers.VatReturnObligationsHttpParser.VatReturnObligationsReads

    val queryString: Seq[(String, String)] = (to, from) match {
      case (Some(dateTo), Some(dateFrom)) => Seq("from" -> dateFrom.toString, "to" -> dateTo.toString, "status" -> status.toString)
      case (_, _) => Seq("status" -> status.toString)
    }

    val httpRequest = http.GET(
      obligationsUrl(vrn),
      queryString
    )(
      implicitly[HttpReads[HttpResult[VatReturnObligations]]],
      headerCarrier(hc),
      implicitly[ExecutionContext]
    )

    httpRequest.map {
      case obligations@Right(_) =>
        obligations
      case httpError@Left(error) =>
        logger.warn("VatObligationsConnector received error: " + error.message)
        httpError
    }
  }
}
