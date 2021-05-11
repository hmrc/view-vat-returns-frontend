/*
 * Copyright 2021 HM Revenue & Customs
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
import stubs.{AuthStub, CustomerInfoStub, VatObligationsStub}

class SubmittedReturnsPageSpec extends IntegrationBaseSpec {

  private trait Test {
    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest("/submitted")
    }

    val obligationsStub = new VatObligationsStub(true)
  }

  "Calling the returns route" when {

    "an authenticated user" should {

      "return an obligation with an end date in 2018" should {

        "return 200" in new Test {
          override def setupStubs(): StubMapping = {
            AuthStub.authorised()
            CustomerInfoStub.stubCustomerInfo
            obligationsStub.stub2018Obligations
          }

          override def request(): WSRequest = {
            setupStubs()
            buildRequest("/submitted")
          }

          val response: WSResponse = await(request().get())
          response.status shouldBe Status.OK
        }

        "return one obligation" in new Test {

          override def setupStubs(): StubMapping = {
            AuthStub.authorised()
            obligationsStub.stub2018Obligations
          }

          override def request(): WSRequest = {
            setupStubs()
            buildRequest("/submitted")
          }

          val response: WSResponse = await(request().get())

          lazy implicit val document: Document = Jsoup.parse(response.body)

          val bulletPointSelector = ".govuk-list--bullet li a"

          document.select(bulletPointSelector).size() shouldBe 1
        }
      }
    }
  }

  "Calling the redirect route" when {

    "user is authorised" should {

      "redirect to /vat-through-software/vat-returns/submitted" in new Test {

        override def setupStubs(): StubMapping = {
          AuthStub.authorised()
        }

        override def request(): WSRequest = {
          setupStubs()
          buildRequest("/submitted/2020")
        }

        val response: WSResponse = await(request().get())

        response.status shouldBe 301
        response.header("Location") shouldBe Some("/vat-through-software/vat-returns/submitted")
      }
    }
  }
}
