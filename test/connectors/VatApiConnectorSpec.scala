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

import controllers.ControllerBaseSpec
import uk.gov.hmrc.play.bootstrap.http.HttpClient

class VatApiConnectorSpec extends ControllerBaseSpec {

  lazy val connector = new VatApiConnector(mock[HttpClient], mockConfig)

  "VatApiConnector" should {

    "generate the correct obligations url" in {
      connector.obligationsUrl("808") shouldBe "/808/obligations"
    }

    "generate the correct customer information url" in {
      connector.customerInfoUrl("123456789") shouldBe "/customer-information/vat/123456789"
    }

    "generate the correct returns url without a period key" in {
      connector.returnUrl("111") shouldBe "/111/returns"
    }

    "generate the correct returns url with a period key" in {
      connector.returnUrl("111", Some("123")) shouldBe "/111/returns/123"
    }
  }
}
