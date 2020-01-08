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

package common

import models.CustomerInformation
import models.customer.CustomerDetail

object TestModels {

  val customerInformationMax = CustomerInformation(
    Some("Cheapo Clothing Ltd"),
    Some("Betty"),
    Some("Jones"),
    Some("Cheapo Clothing"),
    hasFlatRateScheme = true,
    Some(false),
    Some("2018-01-01")
  )

  val customerDetailMax = CustomerDetail(
    "Cheapo Clothing", hasFlatRateScheme = true, isPartialMigration = true, Some("2017-01-01")
  )
}
