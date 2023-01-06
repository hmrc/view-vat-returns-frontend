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

package pages

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationBaseSpec
import play.api.http.Status
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import stubs._

class VatReturnDetailsPageSpec extends IntegrationBaseSpec {

  val obligationsStub = new VatObligationsStub(true)

  val returnsStub = new VatReturnsStub

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

    "the user is authenticated, has an outstanding obligation and a related + " +
      "VAT Return Credit Charge with an outstanding amount owed to the user" should {

      "return 200" in new ReturnRouteTest {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          CustomerInfoStub.stubCustomerInfo
          returnsStub.stubSuccessfulVatReturn
          obligationsStub.stubFulfilledObligations
          FinancialDataStub.stubVatReturnCreditCharge
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }

    "the user is authenticated, has an outstanding obligation and a related + " +
      "VAT Return Debit Charge with an outstanding amount of zero" should {

      "return 200" in new ReturnRouteTest {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          CustomerInfoStub.stubCustomerInfo
          returnsStub.stubSuccessfulVatReturn
          obligationsStub.stubFulfilledObligations
          FinancialDataStub.stubVatReturnDebitCharge(0)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }

    "the user is authenticated, has an outstanding obligation and a related " +
      "VAT AA Return Credit Charge with an outstanding amount owed to the user" should {

      "return 200" in new PaymentReturnRouteTest {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          CustomerInfoStub.stubCustomerInfo
          returnsStub.stubSuccessfulVatReturn
          obligationsStub.stubFulfilledObligations
          FinancialDataStub.stubAAReturnCreditChargeOutstandingPayment
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }

    "the user is authenticated, has an outstanding obligation and a related " +
      "VAT AA Return Debit Charge with an outstanding amount of zero" should {

      "return 200" in new PaymentReturnRouteTest {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          CustomerInfoStub.stubCustomerInfo
          returnsStub.stubSuccessfulVatReturn
          obligationsStub.stubFulfilledObligations
          FinancialDataStub.stubAAReturnDebitChargeOutstandingPayment(0)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }

    "the user is authenticated, has an outstanding obligation and a related " +
      "VAT POA Return Credit Charge with an outstanding amount owed to the user" should {

      "return 200" in new PaymentReturnRouteTest {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          CustomerInfoStub.stubCustomerInfo
          returnsStub.stubSuccessfulVatReturn
          obligationsStub.stubFulfilledObligations
          FinancialDataStub.stubPOAReturnCreditCharge
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }

    "the user is authenticated, has an outstanding obligation and a related " +
      "VAT POA Return Debit Charge with an outstanding amount of zero" should {

      "return 200" in new PaymentReturnRouteTest {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          CustomerInfoStub.stubCustomerInfo
          returnsStub.stubSuccessfulVatReturn
          obligationsStub.stubFulfilledObligations
          FinancialDataStub.stubPOAReturnDebitCharge(0)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }

    "the user is authenticated, has outstanding obligations and no related charge" should {

      "return 200" in new ReturnRouteTest {

        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          CustomerInfoStub.stubCustomerInfo
          returnsStub.stubSuccessfulVatReturn
          obligationsStub.stubFulfilledObligations
          FinancialDataStub.stubNoPayments
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }
  }

  "Calling the /:periodKey route" when {

    "the user is authenticated, has an outstanding obligation and a related " +
      "VAT Return Debit Charge with an outstanding amount owed to HMRC" should {

      "return 200" in new PaymentReturnRouteTest {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          CustomerInfoStub.stubCustomerInfo
          returnsStub.stubSuccessfulVatReturn
          obligationsStub.stubFulfilledObligations
          FinancialDataStub.stubVatReturnDebitCharge(4000)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }

    "the user is authenticated, has an outstanding obligation and a related " +
      "VAT AA Return Debit Charge with an outstanding amount owed to HMRC" should {

      "return 200" in new PaymentReturnRouteTest {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          CustomerInfoStub.stubCustomerInfo
          returnsStub.stubSuccessfulVatReturn
          obligationsStub.stubFulfilledObligations
          FinancialDataStub.stubAAReturnDebitChargeOutstandingPayment(5000)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }

    "the user is authenticated, has an outstanding obligation and a related " +
      "VAT POA Return Debit Charge with an outstanding amount owed to HMRC" should {

      "return 200" in new PaymentReturnRouteTest {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          CustomerInfoStub.stubCustomerInfo
          returnsStub.stubSuccessfulVatReturn
          obligationsStub.stubFulfilledObligations
          FinancialDataStub.stubPOAReturnDebitCharge(5000)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }
  }
}
