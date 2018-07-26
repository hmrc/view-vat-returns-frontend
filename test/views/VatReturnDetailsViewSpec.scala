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

import models.payments.Payment
import models.{VatReturn, VatReturnDetails}
import models.viewModels.VatReturnViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class VatReturnDetailsViewSpec extends ViewBaseSpec {

  object Selectors {
    val pageHeading = "#content h1"
    val subHeading = "#content h2.heading-large"
    val entityNameHeading = "#content h2.heading-medium"
    val mainInformationText = "#content > article > section > p:nth-of-type(1)"
    val extraInformationText = "#content > article > section > p:nth-of-type(2)"
    val tableHeadingOne = "#content section:nth-of-type(1) h3"
    val tableHeadingTwo = "#content section:nth-of-type(2) h3"

    val boxes = List(
      "#box-one", "#box-two", "#box-three",
      "#box-four", "#box-five", "#box-six",
      "#box-seven", "#box-eight", "#box-nine"
    )

    val helpTitle = "details span"
    val helpLink = "details a"
    val helpLine1 = "details p:nth-of-type(1)"
    val helpLine2 = "details p:nth-of-type(2)"
    val helpBullet1 = "details li:nth-of-type(1)"
    val helpBullet2 = "details li:nth-of-type(2)"

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

    val gaTagElement = "#content ul"
    val minusSymbol = "#box-four > div.column-one-quarter.form-hint.text--right > span"
  }

  def boxElement(box: String, column: Int): String = {
    s"$box > div:nth-child($column)"
  }

  val currentYear: Int = 2018

  "Rendering the vat return details page from the returns route with flat rate scheme" should {

    val vatReturnViewModel = VatReturnViewModel(
      Some("Cheapo Clothing"),
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      1000.00,
      LocalDate.parse("2017-04-08"),
      VatReturnDetails(
        VatReturn(
          "#001",
          1297,
          5755,
          7052,
          5732,
          1000,
          77656,
          765765,
          55454,
          545645
        ),
        moneyOwed = false,
        isRepayment = false,
        None
      ),
      showReturnsBreadcrumb = true,
      currentYear,
      hasFlatRateScheme = true,
      hasDirectDebit = false
    )

    lazy val view = views.html.returns.vatReturnDetails(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "Submitted returns"
    }

    "have the correct page heading" in {
      elementText(Selectors.pageHeading) should include ("Submitted returns")
    }

    "render breadcrumbs which" should {

      "have the text 'Business tax account'" in {
        elementText(Selectors.btaBreadcrumb) shouldBe "Business tax account"
      }

      "link to bta" in {
        element(Selectors.btaBreadcrumbLink).attr("href") shouldBe "bta-url"
      }

      "have the text 'VAT'" in {
        elementText(Selectors.vatBreadcrumb) shouldBe "Your VAT details"
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
        elementText(Selectors.currentPage) shouldBe "1 January to 31 March 2017"
      }

    }

    "have the correct subheading" in {
      elementText(Selectors.subHeading) shouldBe "You paid: £1,000"
    }

    "have the correct trading name" in {
      elementText(Selectors.entityNameHeading) shouldBe vatReturnViewModel.entityName.get
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
        "VAT you charged on sales and other supplies",
        "VAT you owe on goods purchased from EC countries and brought into the UK",
        "VAT you owe before deductions (this is the total of box 1 and 2)",
        "VAT you have claimed back",
        "Total VAT you owed",
        "Total value of sales and other supplies, including VAT",
        "Total value of purchases and other expenses, excluding VAT",
        "Total value of supplied goods to EC countries and related costs (excluding VAT)",
        "Total value of goods purchased from EC countries and brought into the UK, as well as any related costs (excluding VAT)"
      )
      expectedDescriptions.indices.foreach(i => elementText(boxElement(Selectors.boxes(i), 2)) shouldBe expectedDescriptions(i))
    }

    "have the minus symbol before the box four amount" in {
      elementText(Selectors.minusSymbol) shouldBe "−"
    }

    "render the correct help revealing link text" in {
      elementText(Selectors.helpTitle) shouldBe "There is an error in my return"
    }

    "render the correct text for the help section first paragraph" in {
      elementText(Selectors.helpLine1) shouldBe
        "You can correct certain errors in your next return, using your accounting software. To do this, "  +
          "the error must have happened in an accounting period that ended in the last 4 years and be either:"
    }

    "render the correct help section report any other errors text" in {
      elementText(Selectors.helpLine2) shouldBe "You must report any other errors (opens in a new tab) to HMRC."
    }

    "render the correct report vat error link text" in {
      elementText(Selectors.helpLink) shouldBe "report any other errors (opens in a new tab)"
    }

    "render the correct report vat error link href" in {
      element(Selectors.helpLink).attr("href") shouldBe "report-vat-error-url"
    }

    "render the correct help section first bullet point text" in {
      elementText(Selectors.helpBullet1) shouldBe "£10,000 or less"
    }

    "render the correct help section second bullet point text" in {
      elementText(Selectors.helpBullet2) shouldBe "1% or less of your box 6 figure and below £50,000"
    }
  }


  "Rendering the vat return details page from the returns route without Flat rate scheme" should {

    val vatReturnViewModel = VatReturnViewModel(
      Some("Cheapo Clothing"),
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      1000.00,
      LocalDate.parse("2017-04-08"),
      VatReturnDetails(
        VatReturn(
          "#001",
          1297,
          5755,
          7052,
          5732,
          1000,
          77656,
          765765,
          55454,
          545645
        ),
        moneyOwed = false,
        isRepayment = false,
        None
      ),
      showReturnsBreadcrumb = true,
      currentYear,
      hasFlatRateScheme = false,
      hasDirectDebit = false
    )

    lazy val view = views.html.returns.vatReturnDetails(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "Submitted returns"
    }

    "have the correct page heading" in {
      elementText(Selectors.pageHeading) should include ("Submitted returns")
    }

    "render breadcrumbs which" should {

      "have the text 'Business tax account'" in {
        elementText(Selectors.btaBreadcrumb) shouldBe "Business tax account"
      }

      "link to bta" in {
        element(Selectors.btaBreadcrumbLink).attr("href") shouldBe "bta-url"
      }

      "have the text 'VAT'" in {
        elementText(Selectors.vatBreadcrumb) shouldBe "Your VAT details"
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
        elementText(Selectors.currentPage) shouldBe "1 January to 31 March 2017"
      }

    }

    "have the correct subheading" in {
      elementText(Selectors.subHeading) shouldBe "You paid: £1,000"
    }

    "have the correct trading name" in {
      elementText(Selectors.entityNameHeading) shouldBe vatReturnViewModel.entityName.get
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
        "VAT you charged on sales and other supplies",
        "VAT you owe on goods purchased from EC countries and brought into the UK",
        "VAT you owe before deductions (this is the total of box 1 and 2)",
        "VAT you have claimed back",
        "Total VAT you owed",
        "Total value of sales and other supplies, excluding VAT",
        "Total value of purchases and other expenses, excluding VAT",
        "Total value of supplied goods to EC countries and related costs (excluding VAT)",
        "Total value of goods purchased from EC countries and brought into the UK, as well as any related costs (excluding VAT)"
      )
      expectedDescriptions.indices.foreach(i => elementText(boxElement(Selectors.boxes(i), 2)) shouldBe expectedDescriptions(i))
    }

    "have the minus symbol before the box four amount" in {
      elementText(Selectors.minusSymbol) shouldBe "−"
    }

    "render the correct help revealing link text" in {
      elementText(Selectors.helpTitle) shouldBe "There is an error in my return"
    }

    "render the correct text for the help section first paragraph" in {
      elementText(Selectors.helpLine1) shouldBe
        "You can correct certain errors in your next return, using your accounting software. To do this, "  +
          "the error must have happened in an accounting period that ended in the last 4 years and be either:"
    }

    "render the correct help section report any other errors text" in {
      elementText(Selectors.helpLine2) shouldBe "You must report any other errors (opens in a new tab) to HMRC."
    }

    "render the correct report vat error link text" in {
      elementText(Selectors.helpLink) shouldBe "report any other errors (opens in a new tab)"
    }

    "render the correct report vat error link href" in {
      element(Selectors.helpLink).attr("href") shouldBe "report-vat-error-url"
    }

    "render the correct help section first bullet point text" in {
      elementText(Selectors.helpBullet1) shouldBe "£10,000 or less"
    }

    "render the correct help section second bullet point text" in {
      elementText(Selectors.helpBullet2) shouldBe "1% or less of your box 6 figure and below £50,000"
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
      VatReturnDetails(
        VatReturn(
          "#001",
          1297,
          5755,
          7052,
          5732,
          1000,
          77656,
          765765,
          55454,
          545645
        ),
        moneyOwed = false,
        isRepayment = false,
        None
      ),
      showReturnsBreadcrumb = false,
      currentYear,
      hasFlatRateScheme = true,
      hasDirectDebit = false
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
      VatReturnDetails(
        VatReturn(
          "#001",
          1297,
          5755,
          7052,
          5732,
          1000.25,
          77656,
          765765,
          55454,
          545645
        ),
        moneyOwed = true,
        isRepayment = false,
        Some(Payment(
          chargeType = "VAT",
          start = LocalDate.parse("2017-01-01"),
          end = LocalDate.parse("2017-03-31"),
          due = LocalDate.parse("2017-04-06"),
          outstandingAmount = 1000.00,
          clearedAmount = 0,
          periodKey = "#001"
        ))
      ),
      showReturnsBreadcrumb = false,
      currentYear,
      hasFlatRateScheme = true,
      hasDirectDebit = false
    )

    lazy val view = views.html.returns.vatReturnDetails(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct subheading" in {
      elementText(Selectors.subHeading) shouldBe "Return total: £1,000.25"
    }

    "have the correct information text under the heading" in {
      elementText(Selectors.mainInformationText) shouldBe "You need to pay this bill by 6 April 2017."
    }

    "have the correct extra information text under the heading" in {
      elementText(Selectors.extraInformationText) shouldBe "It can take up to 7 days to show that you have made a payment."
    }

    "have the correct box 5 description in the table" in {
      elementText(boxElement(Selectors.boxes(4), 2)) shouldBe "Total VAT you owe"
    }

    "have the pay button" in {
      element(Selectors.paymentButton).attr("value") shouldBe "Pay this now"
    }

    "render the correct value for the amount hidden input for the payment service amount" in {
      element(Selectors.paymentServiceDetailAmount).attr("value") shouldBe "100025"
    }

    "render the correct value for the tax period month hidden input for the payment service month" in {
      element(Selectors.paymentServiceDetailMonth).attr("value") shouldBe "3"
    }

    "render the correct value for the tax period year hidden input payment service year" in {
      element(Selectors.paymentServiceDetailYear).attr("value") shouldBe "2017"
    }
  }

  "Rendering the vat return details page when amount (in pence) owed is more than the maximum Integer value (2147483647)" should {

    val vatReturnViewModel = VatReturnViewModel(
      Some("Cheapo Clothing"),
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      1000.00,
      LocalDate.parse("2017-04-08"),
      VatReturnDetails(
        VatReturn(
          "#001",
          1297,
          5755,
          7052,
          5732,
          BigDecimal("21474836.48"),
          77656,
          765765,
          55454,
          545645
        ),
        moneyOwed = true,
        isRepayment = false,
        Some(Payment(
          chargeType = "VAT",
          start = LocalDate.parse("2017-01-01"),
          end = LocalDate.parse("2017-03-31"),
          due = LocalDate.parse("2017-04-06"),
          outstandingAmount = 1000.00,
          clearedAmount = 0,
          periodKey = "#001"
        ))
      ),
      showReturnsBreadcrumb = false,
      currentYear,
      hasFlatRateScheme = true,
      hasDirectDebit = false
    )

    lazy val view = views.html.returns.vatReturnDetails(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the pay button" in {
      element(Selectors.paymentButton).attr("value") shouldBe "Pay this now"
    }

    "render the correct value for the amount hidden input for the payment service amount" in {
      element(Selectors.paymentServiceDetailAmount).attr("value") shouldBe "2147483648"
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
      VatReturnDetails(
        VatReturn(
          "#001",
          1297,
          5755,
          7052,
          5732,
          1000,
          77656,
          765765,
          55454,
          545645
        ),
        moneyOwed = true,
        isRepayment = true,
        Some(Payment(
          chargeType = "VAT",
          start = LocalDate.parse("2017-01-01"),
          end = LocalDate.parse("2017-03-31"),
          due = LocalDate.parse("2017-04-06"),
          outstandingAmount = 1000.00,
          clearedAmount = 0,
          periodKey = "#001"
        ))
      ),
      showReturnsBreadcrumb = false,
      currentYear,
      hasFlatRateScheme = true,
      hasDirectDebit = false
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
      elementText(boxElement(Selectors.boxes(4), 2)) shouldBe "Total VAT HMRC owes you"
    }

    "not have the pay now button" in {
      elementAsOpt(Selectors.paymentButton) shouldBe None
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
      VatReturnDetails(
        VatReturn(
          "#001",
          1297,
          5755,
          7052,
          5732,
          1000,
          77656,
          765765,
          55454,
          545645
        ),
        moneyOwed = false,
        isRepayment = true,
        Some(Payment(
          chargeType = "VAT",
          start = LocalDate.parse("2017-01-01"),
          end = LocalDate.parse("2017-03-31"),
          due = LocalDate.parse("2017-04-06"),
          outstandingAmount = 1000.00,
          clearedAmount = 0,
          periodKey = "#001"
        ))
      ),
      showReturnsBreadcrumb = false,
      currentYear,
      hasFlatRateScheme = true,
      hasDirectDebit = false
    )

    lazy val view = views.html.returns.vatReturnDetails(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct subheading" in {
      elementText(Selectors.subHeading) shouldBe "HMRC paid you: £1,000"
    }

    "have the correct box 5 description in the table" in {
      elementText(boxElement(Selectors.boxes(4), 2)) shouldBe "Total VAT HMRC owed you"
    }

    "not have the pay now button" in {
      elementAsOpt(Selectors.paymentButton) shouldBe None
    }
  }

  "Rendering the vat return details page when a charge hasn't been generated yet" should {

    val vatReturnViewModel = VatReturnViewModel(
      Some("Cheapo Clothing"),
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      1000.00,
      LocalDate.parse("2017-04-08"),
      VatReturnDetails(
        VatReturn(
          "#001",
          1297,
          5755,
          7052,
          5732,
          1000,
          77656,
          765765,
          55454,
          545645
        ),
        moneyOwed = true,
        isRepayment = false,
        None
      ),
      showReturnsBreadcrumb = true,
      currentYear,
      hasFlatRateScheme = true,
      hasDirectDebit = false
    )

    lazy val view = views.html.returns.vatReturnDetails(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct subheading" in {
      elementText(Selectors.subHeading) shouldBe "Return total: £1,000"
    }

    "not have the pay now button" in {
      elementAsOpt(Selectors.paymentButton) shouldBe None
    }

  }

  "Rendering the vat return details page when the user has an active direct debit" should {
    val vatReturnViewModel = VatReturnViewModel(
      Some("Cheapo Clothing"),
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      1000.00,
      LocalDate.parse("2017-04-08"),
      VatReturnDetails(
        VatReturn(
          "#001",
          1297,
          5755,
          7052,
          5732,
          1000,
          77656,
          765765,
          55454,
          545645
        ),
        moneyOwed = true,
        isRepayment = false,
        None
      ),
      showReturnsBreadcrumb = true,
      currentYear,
      hasFlatRateScheme = true,
      hasDirectDebit = true
    )

    lazy val view = views.html.returns.vatReturnDetails(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct subheading" in {
      elementText(Selectors.subHeading) shouldBe "Return total: £1,000"
    }

    "not have the pay now button" in {
      elementAsOpt(Selectors.paymentButton) shouldBe None
    }
  }

  "Rendering the VAT return details page when an entity name is not retrieved" should {

    val vatReturnViewModel = VatReturnViewModel(
      None,
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      1000.00,
      LocalDate.parse("2017-04-08"),
      VatReturnDetails(
        VatReturn(
          "#001",
          1297,
          5755,
          7052,
          5732,
          1000,
          77656,
          765765,
          55454,
          545645
        ),
        moneyOwed = true,
        isRepayment = false,
        None
      ),
      showReturnsBreadcrumb = true,
      currentYear,
      hasFlatRateScheme = true,
      hasDirectDebit = false
    )

    lazy val view = views.html.returns.vatReturnDetails(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "not show the entity name heading" in {
      document.select(Selectors.entityNameHeading) shouldBe empty
    }

    "append a GA tag to the ul element regarding the graceful error handling" in {
      element(Selectors.gaTagElement).attr("data-metrics") shouldBe "error:hidden-text:vat-return-entity-name"
    }
  }
}
