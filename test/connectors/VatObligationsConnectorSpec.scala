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

import controllers.ControllerBaseSpec
import uk.gov.hmrc.http.HttpClient

class VatObligationsConnectorSpec extends ControllerBaseSpec {

  lazy val connector = new VatObligationsConnector(mock[HttpClient], mockConfig)

  "VatObligationsConnector" should {

    "generate the correct returns url with a period key when using vat-obligations" in {
      connector.obligationsUrl("808") shouldBe "/obligations-api/vat-obligations/808/obligations"
    }
  }

}
