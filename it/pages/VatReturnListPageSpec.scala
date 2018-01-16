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

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationBaseSpec
import models.{VatReturnObligation, VatReturnObligations}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.libs.ws.{WSRequest, WSResponse}
import stubs.{AuthStub, VatApiStub}

class VatReturnListPageSpec extends IntegrationBaseSpec {

  private trait Test {
    val vatReturnList = VatReturnObligations(Seq(
      VatReturnObligation(
        LocalDate.parse("2017-10-31"),
        LocalDate.parse("2018-01-31"),
        LocalDate.parse("2018-02-28"),
        "O",
        None,
        "#001"
      ),
      VatReturnObligation(
        LocalDate.parse("2017-07-31"),
        LocalDate.parse("2017-10-31"),
        LocalDate.parse("2017-11-30"),
        "F",
        Some(LocalDate.parse("2017-11-27")),
        "#002"
      ),
      VatReturnObligation(
        LocalDate.parse("2017-04-30"),
        LocalDate.parse("2017-07-31"),
        LocalDate.parse("2017-08-31"),
        "F",
        Some(LocalDate.parse("2017-08-30")),
        "#003"
      ),
      VatReturnObligation(
        LocalDate.parse("2017-01-31"),
        LocalDate.parse("2017-04-30"),
        LocalDate.parse("2017-05-31"),
        "F",
        Some(LocalDate.parse("2017-05-28")),
        "#004"
      )
    ))

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest("/returns")
    }
  }

  "Calling the returns route" when {

    "the user is authenticated and has four obligations" should {

      "return 200" in new Test {
        override def setupStubs(): StubMapping = {
          AuthStub.authorisedVatReturn()
          VatApiStub.stubPrototypeObligations
        }
        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }

      "return the four obligations" in new Test {

        override def setupStubs(): StubMapping = {
          AuthStub.authorisedVatReturn()
          VatApiStub.stubPrototypeObligations
        }
        val response: WSResponse = await(request().get())

        lazy implicit val document: Document = Jsoup.parse(response.body)

        val rowSelector = "#vatReturnsList tbody tr"

        document.select(rowSelector).size() shouldBe 4
      }
    }
  }
}
