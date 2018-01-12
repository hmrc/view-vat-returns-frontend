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

import connectors.httpParsers.CustomerInfoHttpParser.HttpGetResult
import connectors.VatApiConnector
import controllers.ControllerBaseSpec
import models.{CustomerInformation, User}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class VatApiServiceSpec extends ControllerBaseSpec {

  private trait Test {
    val exampleCustomerInfo: CustomerInformation = CustomerInformation("Cheapo Clothing Ltd")
    val mockConnector: VatApiConnector = mock[VatApiConnector]
    val service: VatApiService = new VatApiService(mockConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "Calling .getTradingName" should {

    "return a trading name" in new Test {
      (mockConnector.getTradingName(_: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *)
        .returns(Future.successful(Right(exampleCustomerInfo)))

      lazy val result: HttpGetResult[CustomerInformation] = await(service.getTradingName(User("999999999")))

      result shouldBe Right(exampleCustomerInfo)
    }
  }
}
