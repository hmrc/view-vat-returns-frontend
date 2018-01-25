/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.WireMockMethods
import models.{VatReturnObligation, VatReturnObligations}
import models.errors.{ApiMultiError, ApiSingleError}
import play.api.http.Status._
import play.api.libs.json.Json

object VatApiStub extends WireMockMethods {

  private val obligationsUri = "/vat/([0-9]+)/obligations"
  private val customerInfoApiUri = "/customer-information/vat/([0-9]+)"
  private val dateRegex = "([12]\\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01]))"

  def stubAllObligations: StubMapping = {
    when(method = GET, uri = obligationsUri, queryParams = Map(
      "from" -> dateRegex, "to" -> dateRegex, "status" -> "A"
    ))
      .thenReturn(status = OK, body = Json.toJson(allObligations))
  }

  def stubOutstandingObligations: StubMapping = {
    when(method = GET, uri = obligationsUri, queryParams = Map(
      "from" -> dateRegex, "to" -> dateRegex, "status" -> "O"
    ))
      .thenReturn(status = OK, body = Json.toJson(outstandingObligations))
  }

  def stubFulfilledObligations: StubMapping = {
    when(method = GET, uri = obligationsUri, queryParams = Map(
      "from" -> dateRegex, "to" -> dateRegex, "status" -> "F"
    ))
      .thenReturn(status = OK, body = Json.toJson(fulfilledObligations))
  }

  def stubPrototypeObligations: StubMapping = {
    when(method = GET, uri = obligationsUri, queryParams = Map(
      "from" -> dateRegex, "to" -> dateRegex, "status" -> "A"
    ))
      .thenReturn(status = OK, body = Json.toJson(prototypeObligations))
  }

  def stubNoObligations: StubMapping = {
    when(method = GET, uri = obligationsUri, queryParams = Map(
      "from" -> dateRegex, "to" -> dateRegex, "status" -> "O"
    ))
      .thenReturn(status = OK, body = Json.toJson(noObligations))
  }

  def stubInvalidVrn: StubMapping = {
    when(method = GET, uri = obligationsUri, queryParams = Map(
      "from" -> dateRegex, "to" -> dateRegex, "status" -> "O"
    ))
      .thenReturn(BAD_REQUEST, body = Json.toJson(invalidVrn))
  }

  def stubInvalidFromDate: StubMapping = {
    when(method = GET, uri = obligationsUri, queryParams = Map(
      "from" -> dateRegex, "to" -> dateRegex, "status" -> "F"
    ))
      .thenReturn(BAD_REQUEST, body = Json.toJson(invalidFromDate))
  }

  def stubInvalidToDate: StubMapping = {
    when(method = GET, uri = obligationsUri, queryParams = Map(
      "from" -> dateRegex, "to" -> dateRegex, "status" -> "F"
    ))
      .thenReturn(BAD_REQUEST, body = Json.toJson(invalidToDate))
  }

  def stubInvalidDateRange: StubMapping = {
    when(method = GET, uri = obligationsUri, queryParams = Map(
      "from" -> dateRegex, "to" -> dateRegex, "status" -> "F"
    ))
      .thenReturn(BAD_REQUEST, body = Json.toJson(invalidDateRange))
  }

  def stubInvalidStatus: StubMapping = {
    when(method = GET, uri = obligationsUri, queryParams = Map(
      "from" -> dateRegex, "to" -> dateRegex, "status" -> "F"
    ))
      .thenReturn(BAD_REQUEST, body = Json.toJson(invalidStatus))
  }

  def stubMultipleErrors: StubMapping = {
    when(method = GET, uri = obligationsUri, queryParams = Map(
      "from" -> dateRegex, "to" -> dateRegex, "status" -> "F"
    ))
      .thenReturn(BAD_REQUEST, body = Json.toJson(multipleErrors))
  }

  def stubSuccessfulCustomerInfo: StubMapping = {
    when(method = GET, uri = customerInfoApiUri)
      .thenReturn(status = OK, body = validCustomerInfo)
  }

  def stubFailureCustomerInfo: StubMapping = {
    when(method = GET, uri = customerInfoApiUri)
      .thenReturn(status = BAD_REQUEST, body = Json.toJson(apiError))
  }

  private val pastFulfilledObligation = VatReturnObligation(
    start = LocalDate.now().minusDays(80L),
    end = LocalDate.now().minusDays(50L),
    due = LocalDate.now().minusDays(40L),
    status = "F",
    received = Some(LocalDate.now().minusDays(45L)),
    periodKey = "#001"
  )

  private val pastOutstandingObligation = VatReturnObligation(
    start = LocalDate.now().minusDays(70L),
    end = LocalDate.now().minusDays(40L),
    due = LocalDate.now().minusDays(30L),
    status = "O",
    received = None,
    periodKey = "#004"
  )

  private val allObligations = VatReturnObligations(
    Seq(
      pastFulfilledObligation,
      pastOutstandingObligation
    )
  )

  private val prototypeObligations = VatReturnObligations(Seq(
    VatReturnObligation(
      LocalDate.parse("2017-10-31"),
      LocalDate.parse("2018-01-31"),
      LocalDate.parse("2018-02-28"),
      "O",
      None,
      "#001"
    ),
    VatReturnObligation(
      LocalDate.parse("2017-07-31"),
      LocalDate.parse("2017-10-31"),
      LocalDate.parse("2017-11-30"),
      "F",
      Some(LocalDate.parse("2017-11-27")),
      "#002"
    ),
    VatReturnObligation(
      LocalDate.parse("2017-04-30"),
      LocalDate.parse("2017-07-31"),
      LocalDate.parse("2017-08-31"),
      "F",
      Some(LocalDate.parse("2017-08-30")),
      "#003"
    ),
    VatReturnObligation(
      LocalDate.parse("2017-01-31"),
      LocalDate.parse("2017-04-30"),
      LocalDate.parse("2017-05-31"),
      "F",
      Some(LocalDate.parse("2017-05-28")),
      "#004"
    ))
  )

  private val outstandingObligations = VatReturnObligations(
    allObligations.obligations.filter(_.status == "O")
  )

  private val fulfilledObligations = VatReturnObligations(
    allObligations.obligations.filter(_.status == "F")
  )

  private val noObligations = VatReturnObligations(Seq.empty)

  private val validCustomerInfo = Json.parse(
    """{
      | "organisationDetails":{
      |   "organisationName":"Cheapo Clothing Ltd",
      |   "individualName":{
      |     "firstName":"John",
      |     "lastName":"Smith"
      |   },
      |   "tradingName":"Cheapo Clothing"
      | }
      |}"""
      .stripMargin
  )

  private val apiError: ApiSingleError = ApiSingleError("", "", None)

  private val invalidVrn = ApiSingleError("VRN_INVALID", "", None)
  private val invalidFromDate = ApiSingleError("INVALID_DATE_FROM", "", None)
  private val invalidToDate = ApiSingleError("INVALID_DATE_TO", "", None)
  private val invalidDateRange = ApiSingleError("INVALID_DATE_RANGE", "", None)
  private val invalidStatus = ApiSingleError("INVALID_STATUS", "", None)

  private val multipleErrors = ApiMultiError("BAD_REQUEST", "", Seq(
    ApiSingleError("ERROR_1", "", None),
    ApiSingleError("ERROR_2", "", None)
  ))
}
