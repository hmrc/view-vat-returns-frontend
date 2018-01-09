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

package views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class VatReturnsListViewSpec extends ViewBaseSpec {

  "Rendering the list of your VAT Returns page" should {

    object Selectors {
      val pageHeading = "#content h1"
      val submitThroughSoftware = "#content > article > div > div > p:nth-child(2)"
      val columnOneHeading = "#content > article > div > div > table > thead > tr > th:nth-child(1) > h2"
      val columnTwoHeading = "#content > article > div > div > table > thead > tr > th:nth-child(2) > h2"
      val columnThreeHeading = "#content > article > div > div > table > thead > tr > th:nth-child(3) > h2"
      val earlierReturns = "#content > article > div > div > p:nth-child(6)"
    }

    lazy val view = views.html.vatReturns.vatReturnsList()
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "VAT returns"
    }

    "have the correct page heading" in {
      elementText(Selectors.pageHeading) shouldBe "VAT returns"
    }

    "have the correct message regarding submitting returns through software" in {
      elementText(Selectors.submitThroughSoftware) shouldBe "You submit returns through your accounting software."
    }

    "have the correct heading for column one of the list of VAT returns" in {
      elementText(Selectors.columnOneHeading) shouldBe "Period ending"
    }

    "have the correct heading for column two of the list of VAT returns" in {
      elementText(Selectors.columnTwoHeading) shouldBe "Status"
    }

    "have the correct heading for column three of the list of VAT returns" in {
      elementText(Selectors.columnThreeHeading) shouldBe "Return details"
    }

    "have the correct message regarding viewing ealier returns" in {
      elementText(Selectors.earlierReturns) shouldBe "You can also view earlier returns you submitted before using accounting software."
    }
  }
}
