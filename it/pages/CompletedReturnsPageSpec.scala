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

class CompletedReturnsPageSpec extends IntegrationBaseSpec {

  private trait Test {
    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest("/returns/2017")
    }
  }

  "Calling the returns route with an authenticated user with three obligation end dates in 2017 and one in 2018" when {

    "calling the 2017 route" should {

      "return 200" in new Test {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          VatApiStub.stubPrototypeObligations
        }
        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }

      "return the three obligations" in new Test {

        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          VatApiStub.stubPrototypeObligations
        }
        val response: WSResponse = await(request().get())

        lazy implicit val document: Document = Jsoup.parse(response.body)

        val bulletPointSelector = ".list-bullet li"

        document.select(bulletPointSelector).size() shouldBe 3
      }
    }

    "calling the 2018 route" should {

      "return 200" in new Test {
        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          VatApiStub.stubPrototypeObligations
        }
        override def request(): WSRequest = {
          setupStubs()
          buildRequest("/returns/2018")
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }

      "return one obligation" in new Test {

        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
          VatApiStub.stubPrototypeObligations
        }
        override def request(): WSRequest = {
          setupStubs()
          buildRequest("/returns/2018")
        }

        val response: WSResponse = await(request().get())

        lazy implicit val document: Document = Jsoup.parse(response.body)

        val bulletPointSelector = ".list-bullet li"

        document.select(bulletPointSelector).size() shouldBe 1
      }
    }
  }
}
