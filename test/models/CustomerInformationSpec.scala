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

package models

import java.time.LocalDate
import common.TestModels._
import common.TestJson.customerInfoJsonMax
import mocks.MockAuth

class CustomerInformationSpec extends MockAuth {

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

  ".getExtractDate" when {

    "hybridToFullMigrationDate is available" should {

      "return the date from hybridToFullMigrationDate" in {
          val customerInfo = customerInformationMax
          customerInfo.extractDate shouldBe customerInfo.hybridToFullMigrationDate
      }
    }

    "hybridToFullMigrationDate is missing" should {

       "return customerMigratedToETMPDate" in {
        val customerInfo = customerInformationMax.copy(hybridToFullMigrationDate = None)
        customerInfo.extractDate shouldBe customerInfo.customerMigratedToETMPDate
       }
    }

    "hybridToFullMigrationDate and customerMigratedToETMPDate are unavailable" should {

      "return customerMigratedToETMPDate" in {
        val customerInfo = customerInformationMax.copy(customerMigratedToETMPDate = None, hybridToFullMigrationDate = None)
        customerInfo.extractDate shouldBe customerInfo.customerMigratedToETMPDate
        }
      }

    }

  "calling .isInsolventWithoutAccess" should {

    "return true when the user is insolvent and not continuing to trade" in {
      customerDetailsInsolvent.isInsolventWithoutAccess shouldBe true
    }

    "return false when the user is insolvent but is continuing to trade" in {
      customerDetailsInsolvent.copy(continueToTrade = Some(true)).isInsolventWithoutAccess shouldBe false
    }

    "return false when the user is not insolvent, regardless of the continueToTrade flag" in {
      customerInformationMax.isInsolventWithoutAccess shouldBe false
      customerInformationMax.copy(continueToTrade = Some(false)).isInsolventWithoutAccess shouldBe false
      customerInformationMax.copy(continueToTrade = None).isInsolventWithoutAccess shouldBe false
    }
  }

  "calling .insolvencyDateFutureUserBlocked" should {

    val date  = LocalDate.parse("2018-05-01")

    "return true when the user has a future insolvency date" in {
      callDateService()
      customerInformationFutureInsolvent.insolvencyDateFutureUserBlocked(date) shouldBe true
    }
    "return false when the user has a current or past insolvency date" in {
      callDateService()
      customerInformationFutureInsolvent.copy(insolvencyDate = Some("2018-05-01")).insolvencyDateFutureUserBlocked(date) shouldBe false
    }
    "return false when the user is type 7, 12, 13, 14" in {
      Seq("07","12","13","14").foreach{insolventType =>
        callDateService()
        customerInformationFutureInsolvent.copy(insolvencyType = Some(insolventType)).insolvencyDateFutureUserBlocked(date) shouldBe false
      }
    }
    "return false when the user is not insolvent" in {
      callDateService()
      customerInformationMax.insolvencyDateFutureUserBlocked(date) shouldBe false
    }
  }
}