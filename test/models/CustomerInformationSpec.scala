/*
 * Copyright 2022 HM Revenue & Customs
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

  val exemptInsolvencyTypes: Seq[String] = customerInformationMax.exemptInsolvencyTypes
  val blockedInsolvencyTypes: Seq[String] = customerInformationMax.blockedInsolvencyTypes

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

  "calling .isInsolventWithoutAccess" when {

    "the user is insolvent and has an exempt insolvency type" should {

      "return false" in {
        exemptInsolvencyTypes.foreach { value =>
          customerDetailsInsolvent.copy(insolvencyType = Some(value)).isInsolventWithoutAccess shouldBe false
        }
      }
    }

    "the user is insolvent and has a blocked insolvency type" should {

      "return true" in {
        blockedInsolvencyTypes.foreach { value =>
          customerDetailsInsolvent.copy(insolvencyType = Some(value)).isInsolventWithoutAccess shouldBe true
        }
      }
    }

    "the user is insolvent and has an insolvency type with no associated rules" when {

      "the user is continuing to trade" should {

        "return false" in {
          customerDetailsInsolvent.copy(continueToTrade = Some(true)).isInsolventWithoutAccess shouldBe false
        }
      }

      "the user is not continuing to trade" should {

        "return true" in {
          customerDetailsInsolvent.isInsolventWithoutAccess shouldBe true
        }
      }
    }

    "the user is not insolvent" should {

      "return false" in {
        customerInformationMax.isInsolventWithoutAccess shouldBe false
      }
    }
  }

  "calling .insolvencyDateFutureUserBlocked" should {

    val date  = LocalDate.parse("2018-05-01")

    "return true when the user is insolvent and has a future insolvency date" in {
      callDateService()
      customerInformationFutureInsolvent.insolvencyDateFutureUserBlocked(date) shouldBe true
    }
    "return false when the user has a current or past insolvency date" in {
      callDateService()
      customerInformationFutureInsolvent.copy(insolvencyDate = Some("2018-05-01")).insolvencyDateFutureUserBlocked(date) shouldBe false
    }
    "return false when the user is of an exempt insolvency type, regardless of other flags" in {
      exemptInsolvencyTypes.foreach { value =>
        callDateService()
        customerInformationFutureInsolvent.copy(insolvencyType = Some(value)).insolvencyDateFutureUserBlocked(date) shouldBe false
      }
    }
    "return false when the user is not insolvent" in {
      callDateService()
      customerInformationMax.insolvencyDateFutureUserBlocked(date) shouldBe false
    }
  }
}