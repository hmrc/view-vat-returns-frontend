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

package test.pages

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.FrontendAppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import test.helpers.IntegrationBaseSpec
import test.stubs.{AuthStub, CustomerInfoStub, VatObligationsStub}

class ReturnDeadlinesPageSpec extends IntegrationBaseSpec {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  private trait Test {
    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest("/return-deadlines")
    }

    val obligationsStub = new VatObligationsStub(true)
  }

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  "Calling the return deadlines route with an authenticated user with one obligation" should {

    "return 200" in new Test {
      override def setupStubs(): StubMapping = {
        AuthStub.authorised()
        CustomerInfoStub.stubCustomerInfo
        obligationsStub.stubOutstandingObligations
      }

      val response: WSResponse = await(request().get())
      response.status shouldBe Status.OK
    }

    "return the one deadline" in new Test {
      override def setupStubs(): StubMapping = {
        AuthStub.authorised()
        obligationsStub.stubOutstandingObligations
      }

      val response: WSResponse = await(request().get())
      lazy implicit val document: Document = Jsoup.parse(response.body)
      val deadlineSelector = "hr.govuk-section-break"
      document.select(deadlineSelector).size() shouldBe 1
    }
  }

  "When the user is a Non-MTDfB user" should {

    "return 200" in new Test {
      override def setupStubs(): StubMapping = {
        AuthStub.authorised()
        obligationsStub.stubOutstandingObligations
        CustomerInfoStub.stubOptedOutUser
      }

      val response: WSResponse = await(request().get())
      response.status shouldBe Status.OK
    }

    "return the one deadline with submit link" in new Test {
      override def setupStubs(): StubMapping = {
        AuthStub.authorised()
        obligationsStub.stubOutstandingObligations
        CustomerInfoStub.stubOptedOutUser
      }

      val response: WSResponse = await(request().get())
      lazy implicit val document: Document = Jsoup.parse(response.body)
      val deadlineSelector = "hr.govuk-section-break"

      document.select(deadlineSelector).size() shouldBe 1
      document.getElementById("submit-return-link").text() shouldBe "Submit VAT Return"
      document.getElementById("submit-return-link").attr("href") shouldBe
        "http://localhost:9147/vat-through-software/submit-vat-return/%23004/honesty-declaration"
    }
  }

  "When the user is signed up to MTDfB" should {

    "return 200" in new Test {
      override def setupStubs(): StubMapping = {
        AuthStub.authorised()
        obligationsStub.stubOutstandingObligations
        CustomerInfoStub.stubCustomerInfo
      }

      val response: WSResponse = await(request().get())
      response.status shouldBe Status.OK
    }

    "return the one deadline with no submit link" in new Test {
      override def setupStubs(): StubMapping = {
        AuthStub.authorised()
        obligationsStub.stubOutstandingObligations
        CustomerInfoStub.stubCustomerInfo
      }

      val response: WSResponse = await(request().get())
      lazy implicit val document: Document = Jsoup.parse(response.body)
      val deadlineSelector = "hr.govuk-section-break"
      document.select(deadlineSelector).size() shouldBe 1
      document.getElementById("submit-return-link") shouldBe null
    }

    "return error status when an error is returned from Mandation status" in new Test {
      override def setupStubs(): StubMapping = {
        AuthStub.authorised()
        obligationsStub.stubOutstandingObligations
        CustomerInfoStub.stubErrorFromApi
      }

      val response: WSResponse = await(request().get())
      response.status shouldBe Status.INTERNAL_SERVER_ERROR
    }

  }
}
