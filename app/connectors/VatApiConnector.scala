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
import javax.inject.Inject

import models.VatReturnDetails
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.Future

class VatApiConnector @Inject()(http: HttpClient) {

  // Static example data. Does not look for a specific trading name.
  def getTradingName(vrn: String): Future[String] = {
    Future.successful("Cheapo Clothing Ltd")
  }

  // Static example data return. Does not look for a specific VAT Return.
  def getVatReturnDetails(vrn: String, periodKey: String): Future[VatReturnDetails] = {
    Future.successful(
      VatReturnDetails(
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
}
