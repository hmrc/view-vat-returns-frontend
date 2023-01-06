/*
 * Copyright 2023 HM Revenue & Customs
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
import helpers.WireMockMethods
import models.errors.{ApiMultiError, ApiSingleError}
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsValue, Json}

class VatReturnsStub extends WireMockMethods {

  private val returnsUri = "/vat-returns/returns/vrn/(.+)"

  def stubSuccessfulVatReturn: StubMapping = {
    when(method = GET, uri = returnsUri)
      .thenReturn(status = OK, body = validVatReturn)
  }

  def stubInvalidVrnForReturns: StubMapping = {
    when(method = GET, uri = returnsUri)
      .thenReturn(BAD_REQUEST, body = Json.toJson(invalidVrn))
  }

  def stubInvalidPeriodKey: StubMapping = {
    when(method = GET, uri = returnsUri)
      .thenReturn(BAD_REQUEST, body = Json.toJson(invalidPeriodKey))
  }

  def stubMultipleErrors: StubMapping = {
    when(method = GET, uri = returnsUri)
      .thenReturn(BAD_REQUEST, body = Json.toJson(multipleErrors))
  }

  val validVatReturn: JsValue = Json.parse(
    """
      |{
      |  "periodKey": "#001",
      |  "vatDueSales": 100.00,
      |  "vatDueAcquisitions": 100.00,
      |  "totalVatDue": 200,
      |  "vatReclaimedCurrPeriod": 100.00,
      |  "netVatDue": 100,
      |  "totalValueSalesExVAT": 500,
      |  "totalValuePurchasesExVAT": 500,
      |  "totalValueGoodsSuppliedExVAT": 500,
      |  "totalAcquisitionsExVAT": 500
      |}
    """.stripMargin
  )

  val invalidVrn = ApiSingleError("VRN_INVALID", "")
  val invalidPeriodKey = ApiSingleError("PERIOD_KEY_INVALID", "")
  val multipleErrors = ApiMultiError("BAD_REQUEST", "", Seq(
    ApiSingleError("ERROR_1", "MESSAGE_1"),
    ApiSingleError("ERROR_2", "MESSAGE_2")
  ))
}
