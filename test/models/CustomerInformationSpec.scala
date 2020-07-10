/*
 * Copyright 2020 HM Revenue & Customs
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

package models

import common.TestModels.customerInformationMax
import common.TestJson.customerInfoJsonMax
import uk.gov.hmrc.play.test.UnitSpec

class CustomerInformationSpec extends UnitSpec {

  "A CustomerInformation object" should {

    "be parsed from appropriate JSON" in {
      customerInfoJsonMax.as[CustomerInformation] shouldBe customerInformationMax
    }
  }

  ".getEntityName" when {

    "trading name and organisation name are missing" should {

      "return the first and last name" in {
        val customerInfo = customerInformationMax.copy(tradingName = None, organisationName = None)

        customerInfo.entityName shouldBe customerInfo.firstName.map(_ + " " + customerInfo.lastName.getOrElse("fail"))
      }
    }

    "trading name and individual names are missing" should {

      "return the organisation name" in {
        val customerInfo = customerInformationMax.copy(tradingName = None, firstName = None, lastName = None)

        customerInfo.entityName shouldBe customerInfo.organisationName
      }
    }

    "all names are populated" should {

      "return the trading name" in {
        val customerInfo = customerInformationMax

        customerInfo.entityName shouldBe customerInfo.tradingName
      }
    }

    "no names are populated" should {

      "return None" in {
        val customerInfo =
          customerInformationMax.copy(tradingName = None, organisationName = None, firstName = None, lastName = None)

        customerInfo.entityName shouldBe None
      }
    }
  }
}
