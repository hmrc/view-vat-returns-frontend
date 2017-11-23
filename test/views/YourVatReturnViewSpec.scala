/*
 * Copyright 2017 HM Revenue & Customs
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

class YourVatReturnViewSpec extends ViewBaseSpec {

  "Rendering the vat return details page" should {

    object Selectors {
      val pageHeading = "#content h1"
      val rows = Array("#rowOne", "#rowTwo", "#rowThree", "#rowFour", "#rowFive", "#rowSix", "#rowSeven",
                        "#rowEight", "#rowNine", "#rowTen", "#rowEleven", "#rowTwelve")
      val adjustments = "#adjustments"
    }



    lazy val view = views.html.yourVatReturn()
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "Your VAT return"
    }

    "have the correct page heading" in {
      elementText(Selectors.pageHeading) shouldBe "Your VAT return"
    }

    "have the correct row headings in the table" in {
      val expectedRows = Array("Date submitted:", "Return due date:", "Total sales (minus VAT):", "EU sales (minus VAT):",
                      "VAT charged in UK:", "VAT charged to EU:", "Total costs (minus VAT):", "EU costs (minus VAT):",
                      "Total VAT you charged:", "Total VAT you reclaimed:", "What you owed HMRC:", "Your VAT balance:")

      expectedRows.indices.foreach(i => elementText(Selectors.rows(i)) shouldBe expectedRows(i))
    }

    "have the correct info regarding making adjustments" in {
      elementText(Selectors.adjustments) shouldBe "If there are any errors, you can make adjustments through your software."
    }
  }
}
