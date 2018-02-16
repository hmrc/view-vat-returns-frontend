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

import connectors.VatSubscriptionConnector
import controllers.ControllerBaseSpec
import models.errors.BadRequestError
import models.{CustomerInformation, User}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionServiceSpec extends ControllerBaseSpec {

  private trait Test {
    val mockConnector: VatSubscriptionConnector = mock[VatSubscriptionConnector]
    val service: SubscriptionService = new SubscriptionService(mockConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "Calling .getEntityName" when {

    "the connector retrieves a trading name" should {

      "return the trading name" in new Test {
        val exampleCustomerInfo: CustomerInformation = CustomerInformation(
          Some("My organisation name"),
          Some("John"),
          Some("Smith"),
          Some("My trading name")
        )

        (mockConnector.getCustomerInfo(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Right(exampleCustomerInfo)))

        lazy val result: Option[String] = await(service.getEntityName(User("999999999")))

        result shouldBe Some("My trading name")
      }
    }

    "the connector does not retrieve a trading name or organisation name" should {

      "return the first and last name" in new Test {
        val exampleCustomerInfo: CustomerInformation = CustomerInformation(
          None,
          Some("John"),
          Some("Smith"),
          None
        )

        (mockConnector.getCustomerInfo(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Right(exampleCustomerInfo)))

        val result: Option[String] = await(service.getEntityName(User("999999999")))

        result shouldBe Some("John Smith")
      }
    }

    "the connector does not retrieve a trading name or a first and last name" should {

      "return the organisation name" in new Test {
        val exampleCustomerInfo: CustomerInformation = CustomerInformation(
          Some("My organisation name"),
          None,
          None,
          None
        )

        (mockConnector.getCustomerInfo(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Right(exampleCustomerInfo)))

        val result: Option[String] = await(service.getEntityName(User("999999999")))

        result shouldBe Some("My organisation name")
      }
    }

    "the connector does not retrieve a trading name, organisation name, or individual names" should {

      "return None" in new Test {
        val exampleCustomerInfo: CustomerInformation = CustomerInformation(
          None,
          None,
          None,
          None
        )

        (mockConnector.getCustomerInfo(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Right(exampleCustomerInfo)))

        val result: Option[String] = await(service.getEntityName(User("999999999")))

        result shouldBe None
      }
    }

    "the connector returns an error" should {

      "return None" in new Test {
        (mockConnector.getCustomerInfo(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(Future.successful(Left(BadRequestError("", ""))))

        val result: Option[String] = await(service.getEntityName(User("999999999")))

        result shouldBe None
      }
    }
  }
}