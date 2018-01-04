/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.VatApiConnector
import controllers.ControllerBaseSpec
import models.User

import scala.concurrent.Future

class VatApiServiceSpec extends ControllerBaseSpec {

  private trait Test {
    val exampleTradingName: String = "Cheapo Clothing Ltd"
    val mockConnector: VatApiConnector = mock[VatApiConnector]
    val service: VatApiService = new VatApiService(mockConnector)
  }

  "Calling .getTradingName" should {

    "return a trading name" in new Test {
      (mockConnector.getTradingName(_: String))
        .expects(*)
        .returns(Future.successful(exampleTradingName))

      lazy val result: String = await(service.getTradingName(User("999999999")))

      result shouldBe exampleTradingName
    }
  }
}
