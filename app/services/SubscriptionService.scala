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

import javax.inject.{Inject, Singleton}
import connectors.VatSubscriptionConnector
import models.customer.CustomerDetail
import models.{CustomerInformation, User}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionService @Inject()(connector: VatSubscriptionConnector) {

  def getUserDetails(user: User)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CustomerDetail]] = {
    connector.getCustomerInfo(user.vrn).map {
      case Right(CustomerInformation(None, None, None, None, _)) => None
      case Right(CustomerInformation(None, Some(firstName), Some(lastName), None, hasFlatRateScheme)) =>
        Some(CustomerDetail(s"$firstName $lastName", hasFlatRateScheme))
      case Right(CustomerInformation(organisationName, None, None, None, hasFlatRateScheme)) =>
        organisationName.fold(Option.empty[CustomerDetail])(orgName => Some(CustomerDetail(orgName, hasFlatRateScheme)))
      case Right(CustomerInformation(_, _, _, tradingName, hasFlatRateScheme)) =>
        tradingName.fold(Option.empty[CustomerDetail])(tradeName => Some(CustomerDetail(tradeName, hasFlatRateScheme)))
      case Left(_) => None
    }
  }
}
