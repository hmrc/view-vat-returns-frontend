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
import models.CustomerInformation

object TestModels {

  val customerInformationMax: CustomerInformation = CustomerInformation(
    Some("Cheapo Clothing Ltd"),
    Some("Betty"),
    Some("Jones"),
    Some("Cheapo Clothing"),
    isInsolvent = false,
    Some(true),
    hasFlatRateScheme = true,
    isPartialMigration = false,
    Some("2017-01-01"),
    Some("2017-01-01"),
    mtdfb
  )

  val customerDetailsInsolvent: CustomerInformation = customerInformationMax.copy(isInsolvent = true, continueToTrade = Some(false))

  val customerInformationNonMTDfB: CustomerInformation = customerInformationMax.copy(mandationStatus = nonMTDfB)
  val customerInformationNonDigital: CustomerInformation = customerInformationMax.copy(mandationStatus = nonDigital)
  val customerInformationMTDfBExempt: CustomerInformation = customerInformationMax.copy(mandationStatus = mtdfbExempt)
}
