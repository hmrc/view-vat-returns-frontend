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

import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import config.AppConfig
import connectors.httpParsers.ResponseHttpParsers.HttpResult
import javax.inject.{Inject, Singleton}
import models.VatReturn
import play.api.http.HeaderNames
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import utils.LoggerUtil
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatReturnsConnector @Inject()(http: HttpClient,
                                appConfig: AppConfig) extends LoggerUtil {

  private[connectors] def returnUrl(vrn: String, periodKey: String) = {
    appConfig.vatReturnsBaseUrl + s"/vat-returns/returns/vrn/$vrn?period-key=${URLEncoder.encode(periodKey, UTF_8.name())}"
  }

  private def headerCarrier(hc: HeaderCarrier) = hc.withExtraHeaders(
    HeaderNames.ACCEPT -> "application/vnd.hmrc.1.0+json",
    HeaderNames.CONTENT_TYPE -> "application/json"
  )

  def getVatReturnDetails(vrn: String, periodKey: String)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResult[VatReturn]] = {

    import connectors.httpParsers.VatReturnHttpParser.VatReturnReads

    val httpRequest = http.GET(
      returnUrl(vrn, periodKey)
    )(
      implicitly[HttpReads[HttpResult[VatReturn]]],
      headerCarrier(hc),
      implicitly[ExecutionContext]
    )

    httpRequest.map {
      case nineBox@Right(_) =>
        nineBox
      case httpError@Left(error) =>
        logger.warn("VatReturnsConnector received error: " + error.message)
        httpError
    }
  }
}
