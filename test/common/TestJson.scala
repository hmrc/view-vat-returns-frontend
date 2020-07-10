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

import play.api.libs.json.{JsValue, Json}

object TestJson {

  val customerInfoJsonMax: JsValue = Json.obj(
    "customerDetails" -> Json.obj(
      "organisationName" -> "Cheapo Clothing Ltd",
      "firstName" -> "Betty",
      "lastName" -> "Jones",
      "tradingName" -> "Cheapo Clothing",
      "isPartialMigration" -> false,
      "customerMigratedToETMPDate" -> "2017-01-01"
    ),
    "ppob" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> "Bedrock Quarry"
      )
    ),
    "flatRateScheme" -> Json.obj(
      "FRSCategory" -> "003",
      "FRSPercentage" -> 59.99,
      "limitedCostTrader" -> true
    ),
    "primaryMainCode" -> "10410",
    "mandationStatus" -> "MTDfB"
  )
}
