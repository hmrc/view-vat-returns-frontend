/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.http.Status.{BAD_REQUEST, OK, NOT_FOUND}
import play.api.libs.json.{JsValue, Json}

object FinancialDataStub extends WireMockMethods{

  private val financialDataUri = "/financial-transactions/vat/([0-9]+)"

  def stubAllOutstandingPayments(queryParams: Map[String, String]): StubMapping = {
    when(method = GET, uri = financialDataUri, queryParams = queryParams)
      .thenReturn(status = OK, body = allPayments)
  }

  def stubAAReturnDebitChargeOutstandingPayment(outstandingAmount: BigDecimal): StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = OK, body = aaDebitChargePayment(outstandingAmount))
  }

  def stubAAReturnCreditChargeOutstandingPayment: StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = OK, body = aaCreditChargePayment)
  }

  def stubAllOutstandingPayments: StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = OK, body = allPayments)
  }

  def stubSingleAAInstalmentCharge: StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = OK, body = aaSingleInstalment)
  }

  def stubNoPayments: StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = NOT_FOUND, body = notFound)
  }

  def stubInvalidVrn: StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(BAD_REQUEST, body = invalidVrn)
  }

  private val allPayments: JsValue = Json.parse(
    """{
      |    "idType" : "VRN",
      |    "idNumber" : 555555555,
      |    "regimeType" : "VATC",
      |    "processingDate" : "2017-03-07T09:30:00.000Z",
      |    "financialTransactions" : [
      |      {
      |        "chargeType" : "VAT Return Debit Charge",
      |        "mainType" : "VAT Return Charge",
      |        "periodKey" : "#001",
      |        "periodKeyDescription" : "March 2015",
      |        "taxPeriodFrom" : "2018-05-01",
      |        "taxPeriodTo" : "2018-06-20",
      |        "businessPartner" : "0",
      |        "contractAccountCategory" : "33",
      |        "contractAccount" : "X",
      |        "contractObjectType" : "ABCD",
      |        "contractObject" : "0",
      |        "sapDocumentNumber" : "0",
      |        "sapDocumentNumberItem" : "0",
      |        "chargeReference" : "XD002750002155",
      |        "mainTransaction" : "1234",
      |        "subTransaction" : "1174",
      |        "originalAmount" : 4000,
      |        "outstandingAmount" : 4000,
      |        "clearedAmount" : 0,
      |        "items" : [
      |          {
      |            "subItem" : "000",
      |            "dueDate" : "2018-06-21",
      |            "amount" : 4000
      |          }
      |        ]
      |      },
      |      {
      |        "chargeType" : "VAT Return Debit Charge",
      |        "mainType" : "VAT Return Charge",
      |        "periodKey" : "#002",
      |        "periodKeyDescription" : "March 2015",
      |        "taxPeriodFrom" : "2018-05-01",
      |        "taxPeriodTo" : "2018-06-20",
      |        "businessPartner" : "0",
      |        "contractAccountCategory" : "33",
      |        "contractAccount" : "X",
      |        "contractObjectType" : "ABCD",
      |        "contractObject" : "0",
      |        "sapDocumentNumber" : "0",
      |        "sapDocumentNumberItem" : "0",
      |        "chargeReference" : "XD002750002155",
      |        "mainTransaction" : "1234",
      |        "subTransaction" : "1174",
      |        "originalAmount" : 4000,
      |        "outstandingAmount" : 0,
      |        "clearedAmount" : 4000,
      |        "items" : [
      |          {
      |            "subItem" : "000",
      |            "dueDate" : "2018-06-21",
      |            "amount" : 4000
      |          }
      |        ]
      |      }
      |    ]
      |  }""".stripMargin
  )

  private val aaSingleInstalment: String =
    """{
      |    "chargeType" : "VAT AA Monthly Instalment",
      |    "mainType" : "VAT Annual Accounting",
      |    "periodKey" : "#002",
      |    "taxPeriodFrom" : "2018-05-01",
      |    "originalAmount" : 4000,
      |    "outstandingAmount" : 0,
      |    "clearedAmount" : 4000,
      |    "items" : [
      |      {
      |        "dueDate" : "2018-06-21",
      |        "amount" : 4000,
      |        "clearingDate" : "2018-06-21"
      |      }
      |    ]
      |}"""

  private def aaDebitChargePayment(outstandingAmount: BigDecimal): JsValue = Json.parse(
    s"""{
      |    "idType" : "VRN",
      |    "idNumber" : 555555555,
      |    "regimeType" : "VATC",
      |    "processingDate" : "2017-03-07T09:30:00.000Z",
      |    "financialTransactions" : [
      |      {
      |        "chargeType" : "VAT AA Return Debit Charge",
      |        "mainType" : "VAT AA Return Charge",
      |        "periodKey" : "#001",
      |        "taxPeriodFrom" : "2018-05-01",
      |        "taxPeriodTo" : "2018-06-20",
      |        "originalAmount" : 4000,
      |        "outstandingAmount" : "$outstandingAmount",
      |        "items" : [
      |          {
      |            "subItem" : "000",
      |            "dueDate" : "2018-06-21",
      |            "amount" : 4000
      |          }
      |        ]
      |      },
      |      $aaSingleInstalment
      |    ]
      |  }""".stripMargin
  )

  private val aaCreditChargePayment: JsValue = Json.parse(
    s"""{
      |    "idType" : "VRN",
      |    "idNumber" : 555555555,
      |    "regimeType" : "VATC",
      |    "processingDate" : "2017-03-07T09:30:00.000Z",
      |    "financialTransactions" : [
      |      {
      |        "chargeType" : "VAT AA Return Credit Charge",
      |        "mainType" : "VAT AA Return Charge",
      |        "periodKey" : "#001",
      |        "taxPeriodFrom" : "2018-05-01",
      |        "taxPeriodTo" : "2018-06-20",
      |        "originalAmount" : -4000,
      |        "outstandingAmount" : -2000,
      |        "clearedAmount" : 0,
      |        "items" : [
      |          {
      |            "subItem" : "000",
      |            "dueDate" : "2018-06-21",
      |            "amount" : -2000
      |          }
      |        ]
      |      },
      |      $aaSingleInstalment
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
