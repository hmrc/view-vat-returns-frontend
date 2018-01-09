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

import models.NineBox
import java.time.LocalDate

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class YourNineBoxViewSpec extends ViewBaseSpec {

  "Rendering the vat return details page" should {

    object Selectors {
      val pageHeading = "#content h1"
      val subHeading = "#content h2.heading-large"
      val tradingNameHeading = "#content h2.heading-medium"
      val tableHeadingOne = "#content > article > div > div:nth-child(6) > h3"

      val tableHeadingTwo = "#content > article > div > div:nth-child(14) > div"
      val boxes = Array(
        nineBoxElemSelector("8", "1"), nineBoxElemSelector("9", "1"), nineBoxElemSelector("10", "1"),
        nineBoxElemSelector("11", "1"), nineBoxElemSelector("12", "1"), nineBoxElemSelector("16", "1"),
        nineBoxElemSelector("17", "1"), nineBoxElemSelector("18", "1"), nineBoxElemSelector("19", "1")
      )
      val rowDescriptions = Array(
        nineBoxElemSelector("8", "2"), nineBoxElemSelector("9", "2"), nineBoxElemSelector("10", "2"),
        nineBoxElemSelector("11", "2"), nineBoxElemSelector("12", "2"), nineBoxElemSelector("16", "2"),
        nineBoxElemSelector("17", "2"), nineBoxElemSelector("18", "2"), nineBoxElemSelector("19", "2")
      )
      val adjustments = "#adjustments"
    }

    val exampleVatReturn = NineBox(
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      LocalDate.parse("2017-04-08"),
      1297,
      5755,
      7052,
      5732,
      1320,
      77656,
      765765,
      55454,
      545645
    )
    val tradingName = "Cheapo Clothing Ltd"

    lazy val view = views.html.yourNineBox(exampleVatReturn, tradingName)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "VAT return"
    }

    "have the correct page heading" in {
      elementText(Selectors.pageHeading) should include ("VAT return")
    }

    "have the correct subheading" in {
      elementText(Selectors.subHeading) shouldBe "You owed: £1,320"
    }

    "have the correct trading name" in {
      elementText(Selectors.tradingNameHeading) shouldBe tradingName
    }

    "have the correct heading for the first section of the return" in {
      elementText(Selectors.tableHeadingOne) shouldBe "VAT details"
    }

    "have the correct heading for the second section of the return" in {
      elementText(Selectors.tableHeadingTwo) shouldBe "Additional information"
    }

    "have the correct box numbers in the table" in {
      val expectedBoxes = Array("Box 1", "Box 2", "Box 3", "Box 4", "Box 5", "Box 6", "Box 7", "Box 8", "Box 9")
      expectedBoxes.indices.foreach(i => elementText(Selectors.boxes(i)) shouldBe expectedBoxes(i))
    }

    "have the correct row descriptions in the table" in {
      val expectedDescriptions = Array(
        "VAT on United Kingdom sales and other outputs",
        "VAT on European Community sales and related costs",
        "Total VAT you owe HMRC",
        "Total VAT reclaimed from anywhere",
        "Total you owe",
        "Total sales and other outputs from anywhere, minus VAT",
        "Total purchases from anywhere, minus VAT",
        "Total supplies, goods and related costs to European Community, minus VAT",
        "Total value of acquisitions of goods from European Community, minus VAT"
      )
      expectedDescriptions.indices.foreach(i => elementText(Selectors.rowDescriptions(i)) shouldBe expectedDescriptions(i))
    }

    "have the correct info regarding making adjustments" in {
      elementText(Selectors.adjustments) shouldBe "If there are any errors, you can make adjustments through your software."
    }
  }

  def nineBoxElemSelector(divNumber: String, columnNumber: String): String =
    s"#content > article > div > div:nth-child($divNumber) > div:nth-child($columnNumber)"
}