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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.WireMockMethods
import play.api.http.Status.{OK, BAD_REQUEST}
import play.api.libs.json.{JsValue, Json}

object FinancialDataStub extends WireMockMethods{

  private val financialDataUri = "/financial-transactions/vat/([0-9]+)"

  def stubAllOutstandingPayments: StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = OK, body = allPayments)
  }

  def stubNoPayments: StubMapping = {
    when(method = GET, uri = financialDataUri)
      .thenReturn(status = OK, body = Json.toJson(noPayments))
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

  private val noPayments: JsValue = Json.parse(
    """{
      |    "idType" : "VRN",
      |    "idNumber" : 111111111,
      |    "regimeType" : "VATC",
      |    "processingDate" : "2017-03-07T09:30:00.000Z",
      |    "financialTransactions" : []
      |  }""".stripMargin
  )

  private val invalidVrn = Json.obj(
    "code" -> "INVALID_VRN",
    "reason" -> "VRN was invalid!"
  )
}
