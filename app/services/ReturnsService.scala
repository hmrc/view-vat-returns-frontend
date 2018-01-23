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

import connectors.httpParsers.VatReturnHttpParser.HttpGetResult
import connectors.VatApiConnector
import models.VatReturnObligation.Status
import models.{User, VatReturn, VatReturnObligations}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnsService @Inject()(connector: VatApiConnector) {

  def getVatReturnDetails(user: User, start: LocalDate, end: LocalDate)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpGetResult[VatReturn]] = {
    connector.getVatReturnDetails(user.vrn, start, end)
  }

  def getAllReturns(user: User, upTo: LocalDate)
                   (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpGetResult[VatReturnObligations]] = {
    connector.getVatReturnObligations(vrn = user.vrn, from = upTo.minusYears(1), to = upTo, status = Status.All)
  }
}
