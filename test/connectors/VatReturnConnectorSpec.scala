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

package connectors

import controllers.ControllerBaseSpec
import play.api.libs.json.Json
import uk.gov.hmrc.play.bootstrap.http.HttpClient

class VatReturnConnectorSpec extends ControllerBaseSpec {

  val mockHttp: HttpClient = mock[HttpClient]
  val connector = new VatReturnConnector(mockHttp)

  "Calling the getVatReturn method" should {

    "return some static data" in {

      val expectedReturn =
        Json.toJson(
          """{
            |  "dateSubmitted" : "06-05-2017",
            |  "dueDate" : "08-05-2017",
            |  "totalSales" : 99999,
            |  "euSales" : 77777,
            |  "vatChargedInUk" : 4444,
            |  "vatChargedToEu" : 5555,
            |  "totalCosts" : 999999,
            |  "euCosts" : 9444444,
            |  "totalVatCharged" : 9999,
            |  "totalVatReclaimed" : 7999,
            |  "owedToHmrc" : 999,
            |  "vatBalance" : 0
          """.stripMargin.replace(" ", "")
        )

      val result = await(connector.getVatReturn)

      result shouldEqual expectedReturn
    }
  }
}
