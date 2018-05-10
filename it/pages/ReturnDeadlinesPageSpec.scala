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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.libs.ws.{WSRequest, WSResponse}
import stubs.{AuthStub, VatApiStub}

class ReturnDeadlinesPageSpec extends IntegrationBaseSpec {

  private trait Test {
    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest("/return-deadlines")
    }
  }

  "Calling the return deadlines route with an authenticated user with one obligation" should {

    "return 200" in new Test {
      override def setupStubs(): StubMapping = {
        AuthStub.authorised()
        VatApiStub.stubOutstandingObligations
      }

      val response: WSResponse = await(request().get())
      response.status shouldBe Status.OK
    }

    "return the one deadline" in new Test {
      override def setupStubs(): StubMapping = {
        AuthStub.authorised()
        VatApiStub.stubOutstandingObligations
      }

      val response: WSResponse = await(request().get())
      lazy implicit val document: Document = Jsoup.parse(response.body)
      val deadlineSelector = ".list li.divider--bottom"
      document.select(deadlineSelector).size() shouldBe 1
    }
  }
}
