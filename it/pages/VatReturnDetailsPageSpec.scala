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
import stubs.{AuthStub, FinancialDataStub, VatApiStub}

class VatReturnDetailsPageSpec extends IntegrationBaseSpec {

  private trait ReturnRouteTest {
    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest("/return/%23001?yearEnd=2018")
    }
  }

  private trait PaymentReturnRouteTest {
    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest("/payments/return/%23001?yearEnd=2018")
    }
  }

  "Calling the /return route" when {

    "the user is authenticated and all dependant APIs return a valid response" should {

      "return 200" in new ReturnRouteTest {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          VatApiStub.stubSuccessfulCustomerInfo
          VatApiStub.stubSuccessfulVatReturn
          VatApiStub.stubPrototypeObligations
          FinancialDataStub.stubAllOutstandingPayments
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }

    "the user is not authenticated" should {

      def setupStubsForScenario(): StubMapping = AuthStub.unauthorisedNotLoggedIn()

      "return 401 (Unauthorised)" in new ReturnRouteTest {
        override def setupStubs(): StubMapping = setupStubsForScenario()

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.UNAUTHORIZED
      }
    }

    "the user is not authorised" should {

      def setupStubsForScenario(): StubMapping = AuthStub.unauthorisedOtherEnrolment()

      "return 401 (Forbidden)" in new ReturnRouteTest {
        override def setupStubs(): StubMapping = setupStubsForScenario()

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.FORBIDDEN
      }
    }
  }

  "Calling the /payments/return route" when {

    "the user is authenticated and the Customer Information API returns a valid response" should {

      "return 200" in new PaymentReturnRouteTest {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          VatApiStub.stubSuccessfulCustomerInfo
          VatApiStub.stubSuccessfulVatReturn
          VatApiStub.stubPrototypeObligations
          FinancialDataStub.stubAllOutstandingPayments
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }

    "the user is not authenticated" should {

      def setupStubsForScenario(): StubMapping = AuthStub.unauthorisedNotLoggedIn()

      "return 401 (Unauthorised)" in new PaymentReturnRouteTest {
        override def setupStubs(): StubMapping = setupStubsForScenario()

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.UNAUTHORIZED
      }
    }

    "the user is not authorised" should {

      def setupStubsForScenario(): StubMapping = AuthStub.unauthorisedOtherEnrolment()

      "return 401 (Forbidden)" in new PaymentReturnRouteTest {
        override def setupStubs(): StubMapping = setupStubsForScenario()

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.FORBIDDEN
      }
    }
  }
}
