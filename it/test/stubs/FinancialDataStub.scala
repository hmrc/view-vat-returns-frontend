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

package test.stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{BAD_REQUEST, OK, NOT_FOUND}
import play.api.libs.json.{JsValue, Json}
import test.helpers.WireMockMethods

object FinancialDataStub extends WireMockMethods{

  private val financialDataUri = "/financial-transactions/vat/([0-9]+)"

  def stubAllOutstandingPayments(queryParams: Map[String, String]): StubMapping = {
    when(method = GET, uri = financialDataUri, queryParams = queryParams)
      .thenReturn(status = OK, body = allPayments)
  }

  def stubAAReturnDebitChargeOutstandingPayment(outstandingAmount: BigDecimal): StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = OK, body = generateTransaction("VAT AA Return Debit Charge", outstandingAmount))
  }

  def stubAAReturnCreditChargeOutstandingPayment: StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = OK, body = generateTransaction("VAT AA Return Credit Charge", -100))
  }

  def stubPOAReturnDebitCharge(outstandingAmount: BigDecimal): StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = OK, body = generateTransaction("VAT POA Return Debit Charge", outstandingAmount))
  }

  def stubPOAReturnCreditCharge: StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = OK, body = generateTransaction("VAT POA Return Credit Charge", -100))
  }

  def stubAllOutstandingPayments: StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = OK, body = allPayments)
  }

  def stubVatReturnDebitCharge(outstandingAmount: BigDecimal): StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = OK, body = generateTransaction("VAT Return Debit Charge", outstandingAmount))
  }

  def stubVatReturnCreditCharge: StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = OK, body = generateTransaction("VAT Return Credit Charge", -100))
  }

  def stubNoPayments: StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = NOT_FOUND, body = notFound)
  }

  def stubInvalidVrn: StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(BAD_REQUEST, body = invalidVrn)
  }

  def generateCharge(chargeType: String, periodKey: String, outstandingAmount: BigDecimal): String =
    s"""{
       |   "chargeType" : "$chargeType",
       |   "mainType" : "Blah",
       |   "periodKey" : "$periodKey",
       |   "taxPeriodFrom" : "2018-05-01",
       |   "taxPeriodTo" : "2018-06-20",
       |   "originalAmount" : 4000,
       |   "outstandingAmount" : $outstandingAmount,
       |   "clearedAmount" : 4000,
       |   "items" : [
       |     {
       |       "amount" : 4000,
       |       "dueDate" : "2018-06-21"
       |     }
       |   ]
       |}""".stripMargin

  private def generateTransaction(chargeType: String, outstandingAmount: BigDecimal): JsValue = Json.parse(
    s"""{
       |    "idType" : "VRN",
       |    "idNumber" : 555555555,
       |    "regimeType" : "VATC",
       |    "processingDate" : "2017-03-07T09:30:00.000Z",
       |    "financialTransactions" : [
       |      ${generateCharge(chargeType, "#001", outstandingAmount)},
       |      ${generateCharge("Irrelevant Charge", "#001", 1000)}
       |    ]
       |  }""".stripMargin
  )

  private val allPayments: JsValue = Json.parse(
    s"""{
      |    "idType" : "VRN",
      |    "idNumber" : 555555555,
      |    "regimeType" : "VATC",
      |    "processingDate" : "2017-03-07T09:30:00.000Z",
      |    "financialTransactions" : [
      |      ${generateCharge("VAT Return Debit Charge", "#001", 4000)},
      |      ${generateCharge("VAT Return Debit Charge", "#002", 0)}
      |    ]
      |  }""".stripMargin
  )

  private val invalidVrn = Json.obj(
    "code" -> "INVALID_VRN",
    "reason" -> "VRN was invalid!"
  )

  private val notFound = Json.obj(
    "code" -> "NOT_FOUND",
    "reason" -> "No payments"
  )
}
