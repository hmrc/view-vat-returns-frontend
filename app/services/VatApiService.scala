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

import javax.inject.Inject

import connectors.VatApiConnector
import models.User
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class VatApiService @Inject()(connector: VatApiConnector) {

  def getEntityName(user: User)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    connector.getCustomerInfo(user.vrn).map {
      case Right(customerInfo) =>
        customerInfo.tradingName match {
          case Some(_) => customerInfo.tradingName
          case None => (customerInfo.firstName, customerInfo.lastName) match {
            case (Some(first), Some(last)) => Some(s"$first $last")
            case _ => customerInfo.organisationName
          }
        }
      case Left(_) => None
    }
  }
}
