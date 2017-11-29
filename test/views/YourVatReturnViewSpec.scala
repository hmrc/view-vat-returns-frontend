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

import models.VatReturn
import java.time.LocalDate

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class YourVatReturnViewSpec extends ViewBaseSpec {

  "Rendering the vat return details page" should {

    object Selectors {
      val pageHeading = "#content h1"
      val subHeading = "#content h2"
      val rows = Array("#totalSales", "#euSales", "#vatChargedInUk", "#vatChargedToEu", "#totalCosts",
                    "#euCosts", "#totalVatCharged", "#totalVatReclaimed", "#owedToHmrc", "#vatBalance")
      val adjustments = "#adjustments"
    }

    val exampleVatReturn = VatReturn(
      "ABC Clothing",
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      LocalDate.parse("2017-04-08"),
      99999,
      77777,
      4444,
      5555,
      999999,
      9444444,
      9999,
      7777,
      999.54,
      0
    )

    lazy val view = views.html.yourVatReturn(exampleVatReturn)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "Your VAT return"
    }

    "have the correct page heading" in {
      elementText(Selectors.pageHeading) shouldBe "Your VAT return 1 January to 31 March 2017"
    }

    "have the correct subheading" in {
      elementText(Selectors.subHeading) shouldBe "What you owed: £999.54"
    }

    "have the correct row headings in the table" in {
      val expectedRows = Array("Total sales (minus VAT):", "EU sales (minus VAT):", "VAT charged in UK:",
        "VAT charged to EU:", "Total costs (minus VAT):", "EU costs (minus VAT):", "Total VAT you charged:",
        "Total VAT you reclaimed:", "What you owed HMRC:", "Your VAT balance:")

      expectedRows.indices.foreach(i => elementText(Selectors.rows(i)) shouldBe expectedRows(i))
    }

    "have the correct info regarding making adjustments" in {
      elementText(Selectors.adjustments) shouldBe "If there are any errors, you can make adjustments through your software."
    }
  }
}
