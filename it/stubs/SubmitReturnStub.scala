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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json

object SubmitReturnStub extends WireMockMethods {

  private val mandationUri = "/vat-subscription/([0-9]+)/mandation-status"

  def stubNonMtdfbMandationInfo: StubMapping = {
    when(method = GET, uri = mandationUri)
      .thenReturn(status = OK, body = nonMtdfbUser)
  }

  def stubMtdfbMandationInfo: StubMapping = {
    when(method = GET, uri = mandationUri)
      .thenReturn(status = OK, body = mtdfbUser)
  }

  def stubMandationError: StubMapping = {
    when(method = GET, uri = mandationUri)
      .thenReturn(status = INTERNAL_SERVER_ERROR, body = errorJson)
  }

  private val nonMtdfbUser = Json.parse(
    """{
      |   "mandationStatus" : "Non MTDfB"
      |}""".stripMargin
  )

  private val mtdfbUser = Json.parse(
    """{
      |   "mandationStatus" : "MTDfB Mandated"
      |}""".stripMargin
  )

  private val errorJson = Json.obj(
    "status" -> "500",
    "body" -> "INTERNAL_SERVER_ERROR"
  )

}
