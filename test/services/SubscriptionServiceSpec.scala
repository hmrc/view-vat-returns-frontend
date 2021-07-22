/*
 * Copyright 2021 HM Revenue & Customs
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

import common.TestModels.customerInformationMax
import connectors.VatSubscriptionConnector
import controllers.ControllerBaseSpec
import models.CustomerInformation
import models.errors.BadRequestError
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionServiceSpec extends ControllerBaseSpec {

  val hasFlatRateSchemeNo: Boolean = false
  val hasFlatRateSchemeYes: Boolean = true

  private trait Test {
    val mockConnector: VatSubscriptionConnector = mock[VatSubscriptionConnector]
    val service: SubscriptionService = new SubscriptionService(mockConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "Calling .getUserDetails" when {

    "the connector retrieves customer details" should {

      "return the details" in new Test {

        (mockConnector.getCustomerInfo(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Right(customerInformationMax)))

        lazy val result: Option[CustomerInformation] = await(service.getUserDetails(vrn))

        result shouldBe Some(customerInformationMax)
      }
    }

    "the connector returns an error" should {

      "return None" in new Test {

        (mockConnector.getCustomerInfo(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Left(BadRequestError("", ""))))

        val result: Option[CustomerInformation] = await(service.getUserDetails(vrn))

        result shouldBe None
      }
    }
  }
}
