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

package common

import common.MandationStatuses._
import models.{CustomerInformation, NavContent, NavLinks}

object TestModels {

  val customerInformationMax: CustomerInformation = CustomerInformation(
    Some("Cheapo Clothing Ltd"),
    Some("Betty"),
    Some("Jones"),
    Some("Cheapo Clothing"),
    isInsolvent = false,
    Some(true),
    Some("01"),
    Some("2018-01-01"),
    hasFlatRateScheme = true,
    isPartialMigration = false,
    Some("2018-01-01"),
    Some("2018-02-02"),
    Some("2017-01-01"),
    mtdfb
  )

  val customerInformationMin: CustomerInformation = CustomerInformation(
    None,
    None,
    None,
    None,
    isInsolvent = false,
    None,
    None,
    None,
    hasFlatRateScheme = false,
    isPartialMigration = false,
    None,
    None,
    None,
    mtdfb
  )

  val customerDetailsInsolvent: CustomerInformation =
    customerInformationMax.copy(isInsolvent = true, continueToTrade = Some(false))
  val customerInformationFutureInsolvent: CustomerInformation =
    customerInformationMax.copy(isInsolvent = true, insolvencyDate = Some("2018-05-02"))
  val customerInformationNonMTDfB: CustomerInformation = customerInformationMax.copy(mandationStatus = nonMTDfB)
  val customerInformationNonDigital: CustomerInformation = customerInformationMax.copy(mandationStatus = nonDigital)
  val customerInformationMTDfBExempt: CustomerInformation = customerInformationMax.copy(mandationStatus = mtdfbExempt)
  val customerInformationNoMigDates: CustomerInformation =
    customerInformationMax.copy(customerMigratedToETMPDate = None, hybridToFullMigrationDate = None)

  val navContent: NavContent = NavContent(
    NavLinks("Home", "Hafan", "http://localhost:9999/home"),
    NavLinks("Account", "Crfrif", "http://localhost:9999/account"),
    NavLinks("Messages", "Negeseuon", "http://localhost:9999/messages", Some(1)),
    NavLinks("Help", "Cymorth", "http://localhost:9999/help")
  )
}
