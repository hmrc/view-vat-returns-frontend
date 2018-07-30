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

package pages

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationBaseSpec
import play.api.http.Status
import play.api.libs.ws.{WSRequest, WSResponse}
import stubs._

class VatReturnDetailsPageSpec extends IntegrationBaseSpec {

  val obligationsFeatureEnabled: Boolean =
    app.configuration.underlying.getBoolean("features.useVatObligationsService.enabled")
  val obligationsStub = new VatObligationsStub(obligationsFeatureEnabled)

  val returnsFeatureEnabled: Boolean =
    app.configuration.underlying.getBoolean("features.useVatReturnsService.enabled")
  val returnsStub = new VatReturnsStub(returnsFeatureEnabled)

  private trait ReturnRouteTest {
    def setupStubs(): StubMapping
    def request(): WSRequest = {
      setupStubs()
      buildRequest("/submitted/2018/%23001")
    }
  }

  private trait PaymentReturnRouteTest {
    def setupStubs(): StubMapping
    def request(): WSRequest = {
      setupStubs()
      buildRequest("/%23001")
    }
  }

  "Calling the /submitted/:year/:periodKey route" when {

    "the user is authenticated and all dependent APIs return a valid response" should {

      "return 200" in new ReturnRouteTest {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          CustomerInfoStub.stubCustomerInfo
          returnsStub.stubSuccessfulVatReturn
          obligationsStub.stubFulfilledObligations
          FinancialDataStub.stubAllOutstandingPayments
          FinancialDataStub.stubSuccessfulDirectDebit
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }
  }

  "Calling the /:periodKey route" when {

    "the user is authenticated and all dependent APIs return a valid response" should {

      "return 200" in new PaymentReturnRouteTest {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          CustomerInfoStub.stubCustomerInfo
          returnsStub.stubSuccessfulVatReturn
          obligationsStub.stubFulfilledObligations
          FinancialDataStub.stubAllOutstandingPayments
          FinancialDataStub.stubSuccessfulDirectDebit
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }
  }
}
