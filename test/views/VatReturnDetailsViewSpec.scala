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

import java.time.LocalDate

import models.viewModels.VatReturnViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class VatReturnDetailsViewSpec extends ViewBaseSpec {

  mockConfig.features.allowPayments(true)

  object Selectors {
    val pageHeading = "#content h1"
    val subHeading = "#content h2.heading-large"
    val entityNameHeading = "#content h2.heading-medium"
    val mainInformationText = "#content > article > div.grid-row.column-two-thirds > p:nth-child(2)"
    val extraInformationText = "#content > article > div.grid-row.column-two-thirds > p:nth-child(3)"
    val tableHeadingOne = "#content h3"
    val tableHeadingTwo = "#content > article > div.grid-row.column-two-thirds > div:nth-child(11) > div"

    val boxes = List(
      "#box-one", "#box-two", "#box-three",
      "#box-four", "#box-five", "#box-six",
      "#box-seven", "#box-eight", "#box-nine"
    )

    val adjustments = "#adjustments"
    val paymentButton = ".button"
    val btaBreadcrumb = "div.breadcrumbs li:nth-of-type(1)"
    val btaBreadcrumbLink = "div.breadcrumbs li:nth-of-type(1) a"
    val vatBreadcrumb = "div.breadcrumbs li:nth-of-type(2)"
    val vatBreadcrumbLink = "div.breadcrumbs li:nth-of-type(2) a"
    val previousPageBreadcrumb = "div.breadcrumbs li:nth-of-type(3)"
    val previousPageBreadcrumbLink = "div.breadcrumbs li:nth-of-type(3) a"
    val currentPage = "div.breadcrumbs li:nth-of-type(4)"

    val paymentServiceDetailAmount = "#payment-detail input:nth-of-type(1)"
    val paymentServiceDetailMonth = "#payment-detail input:nth-of-type(2)"
    val paymentServiceDetailYear = "#payment-detail input:nth-of-type(3)"
  }

  def boxElement(box: String, column: Int): String = {
    val nameSelector = if (box.equals("#box-five")) "strong" else "div"
    s"$box > $nameSelector:nth-child($column)"
  }

  val currentYear: Int = 2018

  "Rendering the vat return details page from the returns route" should {

    val vatReturnViewModel = VatReturnViewModel(
      Some("Cheapo Clothing"),
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      1000.00,
      LocalDate.parse("2017-04-08"),
      1297,
      5755,
      7052,
      5732,
      1000,
      77656,
      765765,
      55454,
      545645,
      moneyOwed = false,
      isRepayment = false,
      showReturnsBreadcrumb = true,
      currentYear = currentYear
    )
    lazy val view = views.html.returns.vatReturnDetails(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "VAT return"
    }

    "have the correct page heading" in {
      elementText(Selectors.pageHeading) should include ("VAT return")
    }

    "render breadcrumbs which" should {

      "have the text 'Business tax account'" in {
        elementText(Selectors.btaBreadcrumb) shouldBe "Business tax account"
      }

      "link to bta" in {
        element(Selectors.btaBreadcrumbLink).attr("href") shouldBe "bta-url"
      }

      "have the text 'VAT'" in {
        elementText(Selectors.vatBreadcrumb) shouldBe "VAT"
      }

      s"link to 'vat-details-url'" in {
        element(Selectors.vatBreadcrumbLink).attr("href") shouldBe "vat-details-url"
      }

      "have the text 'Submitted returns'" in {
        elementText(Selectors.previousPageBreadcrumb) shouldBe "Submitted returns"
      }

      s"link to ${controllers.routes.ReturnObligationsController.submittedReturns(currentYear).url}" in {
        element(Selectors.previousPageBreadcrumbLink).attr("href") shouldBe
          controllers.routes.ReturnObligationsController.submittedReturns(currentYear).url
      }

      "have the correct current page text containing the obligation dates" in {
        elementText(Selectors.currentPage) shouldBe "VAT return: 1 January to 31 March 2017"
      }

    }

    "have the correct subheading" in {
      elementText(Selectors.subHeading) shouldBe "You paid: £1,000"
    }

    "have the correct trading name" in {
      elementText(Selectors.entityNameHeading) shouldBe vatReturnViewModel.entityName.get
    }

    "have the correct information text under the heading" in {
      elementText(Selectors.mainInformationText) shouldBe
        "Your payment has been processed. There is nothing more you need to do with this return."
    }

    "have the correct heading for the first section of the return" in {
      elementText(Selectors.tableHeadingOne) shouldBe "VAT details"
    }

    "have the correct heading for the second section of the return" in {
      elementText(Selectors.tableHeadingTwo) shouldBe "Additional information"
    }

    "have the correct box numbers in the table" in {
      val expectedBoxes = Array("Box 1", "Box 2", "Box 3", "Box 4", "Box 5", "Box 6", "Box 7", "Box 8", "Box 9")
      expectedBoxes.indices.foreach(i => elementText(boxElement(Selectors.boxes(i), 1)) shouldBe expectedBoxes(i))
    }

    "have the correct row descriptions in the table" in {
      val expectedDescriptions = Array(
        "VAT on United Kingdom sales and other outputs",
        "VAT on European Community sales and related costs",
        "VAT sales subtotal",
        "Total VAT reclaimed from anywhere",
        "Total you owed",
        "Total sales and other outputs from anywhere, minus VAT",
        "Total purchases from anywhere, minus VAT",
        "Total supplies, goods and related costs to European Community, minus VAT",
        "Total value of acquisitions of goods from European Community, minus VAT"
      )
      expectedDescriptions.indices.foreach(i => elementText(boxElement(Selectors.boxes(i), 2)) shouldBe expectedDescriptions(i))
    }

    "have the correct info regarding making adjustments" in {
      elementText(Selectors.adjustments) shouldBe "If there are any errors, add or subtract them in your next return."
    }
  }

  "Rendering the vat return details page from the payments route" should {

    val vatReturnViewModel = VatReturnViewModel(
      Some("Cheapo Clothing"),
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      1000.00,
      LocalDate.parse("2017-04-08"),
      1297,
      5755,
      7052,
      5732,
      1320,
      77656,
      765765,
      55454,
      545645,
      moneyOwed = false,
      isRepayment = false,
      showReturnsBreadcrumb = false,
      currentYear = currentYear
    )
    lazy val view = views.html.returns.vatReturnDetails(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "render a breadcrumb for the payments page" should {

      "have the text 'What you owe'" in {
        elementText(Selectors.previousPageBreadcrumb) shouldBe "What you owe"
      }

      s"link to 'vat-payments-url'" in {
        element(Selectors.previousPageBreadcrumbLink).attr("href") shouldBe "vat-payments-url"
      }
    }
  }

  "Rendering the vat return details page when money is owed on the return" should {

    val vatReturnViewModel = VatReturnViewModel(
      Some("Cheapo Clothing"),
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      1000.00,
      LocalDate.parse("2017-04-08"),
      1297,
      5755,
      7052,
      5732,
      1000.25,
      77656,
      765765,
      55454,
      545645,
      moneyOwed = true,
      isRepayment = false,
      showReturnsBreadcrumb = false,
      currentYear = currentYear
    )
    lazy val view = views.html.returns.vatReturnDetails(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct subheading" in {
      elementText(Selectors.subHeading) shouldBe "Return total: £1,000.25"
    }

    "have the correct information text under the heading" in {
      elementText(Selectors.mainInformationText) shouldBe "This bill needs to be paid before 6 April 2017."
    }

    "have the correct extra information text under the heading" in {
      elementText(Selectors.extraInformationText) shouldBe "Payments can take between 4 and 7 days to appear here."
    }

    "have the correct box 5 description in the table" in {
      elementText(boxElement(Selectors.boxes(4), 2)) shouldBe "Total you owe"
    }

    "have the pay button" in {
      element(Selectors.paymentButton).attr("value") shouldBe "Pay this now"
    }

    "render the correct value for the amount hidden input for the  payment service amount" in {
      element(Selectors.paymentServiceDetailAmount).attr("value") shouldBe "100025"
    }

    "render the correct value for the tax period month hidden input for the  payment service month" in {
      element(Selectors.paymentServiceDetailMonth).attr("value") shouldBe "03"
    }

    "render the correct value for the tax period year hidden input payment service year" in {
      element(Selectors.paymentServiceDetailYear).attr("value") shouldBe "17"
    }
  }

  "Rendering the vat return details page when HMRC owe money on the return" should {

    val vatReturnViewModel = VatReturnViewModel(
      Some("Cheapo Clothing"),
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      1000.00,
      LocalDate.parse("2017-04-08"),
      1297,
      5755,
      7052,
      5732,
      1000,
      77656,
      765765,
      55454,
      545645,
      moneyOwed = true,
      isRepayment = true,
      showReturnsBreadcrumb = false,
      currentYear = currentYear
    )
    lazy val view = views.html.returns.vatReturnDetails(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct subheading" in {
      elementText(Selectors.subHeading) shouldBe "HMRC will pay you: £1,000"
    }

    "have the correct information text under the heading" in {
      elementText(Selectors.mainInformationText) shouldBe "It can take up to 30 days for you to receive a repayment."
    }

    "have the correct box 5 description in the table" in {
      elementText(boxElement(Selectors.boxes(4), 2)) shouldBe "HMRC will pay you"
    }
  }

  "Rendering the vat return details page when HMRC have paid what they owe on the return" should {

    val vatReturnViewModel = VatReturnViewModel(
      Some("Cheapo Clothing"),
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      1000.00,
      LocalDate.parse("2017-04-08"),
      1297,
      5755,
      7052,
      5732,
      1000,
      77656,
      765765,
      55454,
      545645,
      moneyOwed = false,
      isRepayment = true,
      showReturnsBreadcrumb = false,
      currentYear = currentYear
    )
    lazy val view = views.html.returns.vatReturnDetails(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct subheading" in {
      elementText(Selectors.subHeading) shouldBe "HMRC paid you: £1,000"
    }

    "have the correct information text under the heading" in {
      elementText(Selectors.mainInformationText) shouldBe
        "Your payment has been processed. There is nothing more you need to do with this return."
    }

    "have the correct box 5 description in the table" in {
      elementText(boxElement(Selectors.boxes(4), 2)) shouldBe "Total amount HMRC owed you"
    }
  }
}