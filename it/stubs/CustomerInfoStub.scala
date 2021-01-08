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

package stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.MandationStatuses.{mtdfb, nonMTDfB}
import helpers.WireMockMethods
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json

object CustomerInfoStub extends WireMockMethods {

  private val customerInfoUri = "/vat-subscription/([0-9]+)/full-information"

  def stubCustomerInfo: StubMapping =
    when(method = GET, uri = customerInfoUri)
      .thenReturn(status = OK, body = customerInfo(mtdfb))

  def stubOptedOutUser: StubMapping =
    when(method = GET, uri = customerInfoUri)
      .thenReturn(status = OK, body = customerInfo(nonMTDfB))

  def stubErrorFromApi: StubMapping =
    when(method = GET, uri = customerInfoUri)
      .thenReturn(status = INTERNAL_SERVER_ERROR, body = errorJson)

  private def customerInfo(mandationStatus: String) = Json.obj(
    "customerDetails" -> Json.obj(
      "organisationName" -> "Cheapo Clothing Ltd",
      "firstName" -> "Vincent",
      "lastName" -> "Vatreturn",
      "tradingName" -> "Cheapo Clothing",
      "isInsolvent" -> false,
      "continueToTrade" -> true,
      "isPartialMigration" -> true,
      "customerMigratedToETMPDate" -> "2018-01-01",
      "hybridToFullMigrationDate" -> "2018-01-01"
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
    "mandationStatus" -> mandationStatus
  )

  private val errorJson = Json.obj(
    "code" -> "500",
    "message" -> "INTERNAL_SERVER_ERROR"
  )
}
