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

import java.time.LocalDate

import connectors.VatReturnConnector
import controllers.ControllerBaseSpec
import models.User
import models.VatReturn

import scala.concurrent.Future

class VatReturnServiceSpec extends ControllerBaseSpec {

  private trait Test {
    val exampleVatReturn: VatReturn = VatReturn(
      "ABC Clothing",
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      LocalDate.parse("2017-04-08"),
      99999,
      77777,
      4444,
      5555,
      999999,
      9444444,
      9999,
      7777,
      999.54,
      0
    )
    val mockConnector: VatReturnConnector = mock[VatReturnConnector]
    val service = new VatReturnService(mockConnector)
  }

  "Calling .getVatReturn" should {

    "return a VAT Return" in new Test {
      (mockConnector.getVatReturn(_: String))
        .expects(*)
        .returns(Future.successful(exampleVatReturn))

      lazy val result: VatReturn = await(service.getVatReturn(User("999999999")))

      result shouldBe exampleVatReturn
    }
  }
}
