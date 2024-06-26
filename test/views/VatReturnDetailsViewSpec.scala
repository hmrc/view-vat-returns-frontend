/*
 * Copyright 2024 HM Revenue & Customs
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
import org.scalatest.BeforeAndAfterEach
import org.scalatest.exceptions.TestFailedException
import views.html.returns.VatReturnDetailsView

class VatReturnDetailsViewSpec extends ViewBaseSpec with BeforeAndAfterEach {

  val injectedView: VatReturnDetailsView = inject[VatReturnDetailsView]

  object Selectors {
    val pageHeading = "#content h1"
    val subHeadingP1 = ".return-total"
    val subHeadingP2 = ".owe-hmrc"
    val mainInformationText = "#content > section > div > p:nth-of-type(1)"
    val extraInformationText = "#content > section > div > p:nth-of-type(2)"
    val tableHeadingOne = "#vat-details"
    val tableHeadingTwo = "#additional-information"

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

    val btaBreadcrumb = "ol.govuk-breadcrumbs__list li:nth-of-type(1)"
    val btaBreadcrumbLink = "ol.govuk-breadcrumbs__list li:nth-of-type(1) a"
    val vatBreadcrumb = "ol.govuk-breadcrumbs__list li:nth-of-type(2)"
    val vatBreadcrumbLink = "ol.govuk-breadcrumbs__list li:nth-of-type(2) a"
    val previousPageBreadcrumb = "ol.govuk-breadcrumbs__list li:nth-of-type(3)"
    val previousPageBreadcrumbLink = "ol.govuk-breadcrumbs__list li:nth-of-type(3) a"

    val backLink = ".govuk-back-link"

    val gaTagElement = "#content ul"
    val minusSymbol = "#box-four > dd.govuk-summary-list__actions.vatvc-grey-paragraph-text"

    def boxTitle(box: String): String = s"$box > dt"
    def boxDescription(box: String): String = s"$box > dd:nth-of-type(1)"
  }

  val currentYear: Int = 2018
  val payment = Payment(
    "VAT Return Debit Charge",
    LocalDate.parse("2017-01-01"),
    LocalDate.parse("2017-03-31"),
    LocalDate.parse("2017-04-05"),
    1000.00,
    "#001"
  )
  val returnDetails: VatReturnDetails = VatReturnDetails(
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
    oweHmrc = Some(true),
    Some(payment)
  )

  val vatReturnViewModel: VatReturnViewModel = VatReturnViewModel(
    Some("Cheapo Clothing"),
    LocalDate.parse("2017-01-01"),
    LocalDate.parse("2017-03-31"),
    LocalDate.parse("2017-04-06"),
    1000.00,
    LocalDate.parse("2017-04-08"),
    returnDetails,
    showReturnsBreadcrumb = true,
    currentYear,
    hasFlatRateScheme = true,
    isHybridUser = false
  )

  "Rendering the vat return details page from the returns route with flat rate scheme enabled" should {

    lazy val view = injectedView(vatReturnViewModel)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "Submitted return for 1 January to 31 March 2017 - Manage your VAT account - GOV.UK"
    }

    "have the correct document title using non breaking space" in {
      document.title.contains("Submitted return for 1\u00a0January to 31\u00a0March\u00a02017 - Manage your VAT account - GOV.UK")
    }

    "have the correct page heading" in {
      elementText(Selectors.pageHeading) should include("Submitted return for Cheapo Clothing")
    }

    "render breadcrumbs which" should {

      "have the text 'Business tax account'" in {
        elementText(Selectors.btaBreadcrumb) shouldBe "Business tax account"
      }

      "link to bta" in {
        element(Selectors.btaBreadcrumbLink).attr("href") shouldBe "bta-url"
      }

      "have the text 'VAT'" in {
        elementText(Selectors.vatBreadcrumb) shouldBe "Your VAT account"
      }

      s"link to 'vat-details-url'" in {
        element(Selectors.vatBreadcrumbLink).attr("href") shouldBe "vat-details-url"
      }

      "have the text 'Submitted returns'" in {
        elementText(Selectors.previousPageBreadcrumb) shouldBe "Submitted returns"
      }

      s"link to ${controllers.routes.SubmittedReturnsController.submittedReturns.url}" in {
        element(Selectors.previousPageBreadcrumbLink).attr("href") shouldBe
          controllers.routes.SubmittedReturnsController.submittedReturns.url
      }
    }

    "have the correct subheading" in {
      elementText(Selectors.subHeadingP1) shouldBe "Return total: £1,000"
      elementText(Selectors.subHeadingP2) shouldBe "You owe HMRC: £1,000"
    }

    "have the correct heading for the first section of the return" in {
      elementText(Selectors.tableHeadingOne) shouldBe "VAT details"
    }

    "have the correct heading for the second section of the return" in {
      elementText(Selectors.tableHeadingTwo) shouldBe "Additional information"
    }

    "have the correct box numbers in the table" in {
      val expectedBoxes = Array("Box 1", "Box 2", "Box 3", "Box 4", "Box 5", "Box 6", "Box 7", "Box 8", "Box 9")
      expectedBoxes.indices.foreach(i => elementText(Selectors.boxTitle(Selectors.boxes(i))) shouldBe expectedBoxes(i))
    }

    "have the correct row descriptions in the table" in {
      val expectedDescriptions = Array(
        "VAT due in the period on sales and other outputs",
        "VAT due in the period on acquisitions of goods made in Northern Ireland from EU Member States",
        "Total VAT due (this is the total of box 1 and 2)",
        "VAT reclaimed in the period on purchases and other inputs (including acquisitions in Northern Ireland from EU member states)",
        "Net VAT to pay to HMRC or reclaim (this is the difference between box 3 and 4)",
        "Total value of sales and other supplies, including VAT",
        "Total value of purchases and all other inputs excluding any VAT",
        "Total value of dispatches of goods and related costs (excluding VAT) from Northern Ireland to EU Member States",
        "Total value of acquisitions of goods and related costs (excluding VAT) made in Northern Ireland from EU Member States"
      )
      expectedDescriptions.indices.foreach(i => elementText(Selectors.boxDescription(Selectors.boxes(i))) shouldBe
        expectedDescriptions(i))
    }

    "have the minus symbol before the box four amount" in {
      elementText(Selectors.minusSymbol) shouldBe "− £5,732"
    }

    "have a print button" in {
      elementText("#print-return-button") shouldBe "Print VAT Return"
    }

    "render the correct help revealing link text" in {
      elementText(Selectors.helpTitle) shouldBe "There is an error in my return"
    }

    "render the correct text for the help section first paragraph" in {
      elementText(Selectors.helpLine1) shouldBe
        "You can correct some errors in your next return. The error must have " +
          "happened in an accounting period that ended in the last 4 years and be either:"
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

  "Rendering the vat return details page from the returns route with flat rate scheme for an agent" should {

    lazy val view = injectedView(vatReturnViewModel)(
      fakeRequestWithClientsVRN, messages, mockConfig, agentUser)

    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "Submitted return for 1 January to 31 March 2017 - Your client’s VAT details - GOV.UK"
    }

    "have the correct document title using non breaking space" in {
      document.title.contains("Submitted return for 1\u00a0January to 31\u00a0March\u00a02017 - Your client’s VAT details - GOV.UK")
    }

    "not render breadcrumbs which" in {
      a[TestFailedException] should be thrownBy element(Selectors.btaBreadcrumb)
      a[TestFailedException] should be thrownBy element(Selectors.btaBreadcrumbLink)
      a[TestFailedException] should be thrownBy element(Selectors.vatBreadcrumb)
      a[TestFailedException] should be thrownBy element(Selectors.vatBreadcrumbLink)
      a[TestFailedException] should be thrownBy element(Selectors.previousPageBreadcrumb)
      a[TestFailedException] should be thrownBy element(Selectors.previousPageBreadcrumbLink)
    }

    "render a back link" in {
      elementText(Selectors.backLink) shouldBe "Back"
    }

    "have a print button" in {
      elementText("#print-return-button") shouldBe "Print VAT Return"
    }
  }


  "Rendering the vat return details page from the returns route without Flat rate scheme" should {

    val vatReturnViewModelWithoutFlatRate = vatReturnViewModel.copy(hasFlatRateScheme = false)

    lazy val view = injectedView(vatReturnViewModelWithoutFlatRate)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct box 6 description in the table" in {
      val expectedDescription = "Total value of sales and other supplies, excluding VAT"
      elementText(Selectors.boxDescription(Selectors.boxes(5))) shouldBe expectedDescription
    }
  }


  "Rendering the vat return details page from the payments route" should {

    val vatReturnViewModelNoOwed = vatReturnViewModel.copy(
      vatReturnDetails = returnDetails.copy(moneyOwed = false, payment = None),
      showReturnsBreadcrumb = false
    )

    lazy val view = injectedView(vatReturnViewModelNoOwed)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "render a breadcrumb for the payments page" should {

      "have the text 'What you owe'" in {
        elementText(Selectors.previousPageBreadcrumb) shouldBe "What you owe"
      }

      s"link to 'vat-payments-url'" in {
        element(Selectors.previousPageBreadcrumbLink).attr("href") shouldBe "vat-payments-url"
      }
    }

    "not render back button" in {
      an[TestFailedException] should be thrownBy element(Selectors.backLink)
    }
  }

  "Rendering the vat return details page when money is owed on the return" should {

    val vatReturnViewModelMoneyOwed = vatReturnViewModel.copy(
      vatReturnDetails = returnDetails.copy(vatReturn =
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
        )
      ),
      showReturnsBreadcrumb = false
    )

    lazy val view = injectedView(vatReturnViewModelMoneyOwed)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct subheading" in {
      elementText(Selectors.subHeadingP1) shouldBe "Return total: £1,000.25"
      elementText(Selectors.subHeadingP2) shouldBe "You owe HMRC: £1,000"
    }

    "have the correct information text under the heading" in {
      elementText(Selectors.mainInformationText) shouldBe
        "We have worked out what you owe based on any payments you might have made on your account."
    }

    "have the correct extra information text under the main information text" in {
      elementText(Selectors.extraInformationText) shouldBe
        "You need to pay this bill by 6 April 2017. It can take up to 7 days to show that you have made a payment."
    }

    "have the correct extra information text under the main information text using non breaking space" in {
      elementText(Selectors.extraInformationText)
        .contains("You need to pay this bill by 6\u00a0April\u00a02017. It can take up to 7 days to show that you have made a payment.")
    }
  }

  "Rendering the vat return details page when HMRC owe money on the return" should {

    val vatReturnViewModelHmrcOwes = vatReturnViewModel.copy(
      vatReturnDetails = returnDetails.copy(oweHmrc = Some(false), payment =
        Some(Payment(
          chargeType = "VAT",
          periodFrom = LocalDate.parse("2017-01-01"),
          periodTo = LocalDate.parse("2017-03-31"),
          due = LocalDate.parse("2017-04-06"),
          outstandingAmount = 1000.00,
          periodKey = "#001"
        ))
      ),
      showReturnsBreadcrumb = false
    )

    lazy val view = injectedView(vatReturnViewModelHmrcOwes)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct subheading" in {
      elementText(Selectors.subHeadingP1) shouldBe "Return total: £1,000"
      elementText(Selectors.subHeadingP2) shouldBe "HMRC owes you: £1,000"
    }

    "have the correct information text under the heading" in {
      elementText(Selectors.extraInformationText) shouldBe "It can take up to 30 days for you to receive a repayment."
    }
  }

  "Rendering the vat return details page when HMRC have paid what they owe on the return" should {

    val vatReturnViewModelHmrcPaid = vatReturnViewModel.copy(
      vatReturnDetails = returnDetails.copy(
        moneyOwed = false,
        oweHmrc = Some(false),
        payment = Some(payment.copy(
          outstandingAmount = 0
        ))
      ),
      showReturnsBreadcrumb = false
    )

    lazy val view = injectedView(vatReturnViewModelHmrcPaid)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct subheading" in {
      elementText(Selectors.subHeadingP1) shouldBe "Return total: £1,000"
      elementText(Selectors.subHeadingP2) shouldBe "You owe HMRC: £0"
    }
  }

  "Rendering the vat return details page when a charge hasn't been generated yet" should {

    val vatReturnViewModelNoCharge = vatReturnViewModel.copy(
      vatReturnDetails = returnDetails.copy(oweHmrc = None, payment = None)
    )

    lazy val view = injectedView(vatReturnViewModelNoCharge)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct subheading" in {
      elementText(Selectors.subHeadingP1) shouldBe "Return total: £1,000"
    }
  }


  "Rendering the VAT return details page when an entity name is not retrieved" should {

    val vatReturnViewModelNoEntityName = vatReturnViewModel.copy(entityName = None)

    lazy val view = injectedView(vatReturnViewModelNoEntityName)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "not show the entity name in the page heading" in {
      elementText(Selectors.pageHeading) shouldBe "Submitted return for 1 Jan to 31 Mar 2017"
    }

    "not show the entity name in the page heading using non breaking space" in {
      elementText(Selectors.pageHeading).contains("Submitted return for 1\u00a0Jan to 31\u00a0Mar\u00a02017")
    }
  }

  "Rendering the VAT return details page when the period key is 9999 (Final Return)" should {

    val vatReturnViewModelFinalReturn = vatReturnViewModel.copy(
      vatReturnDetails = returnDetails.copy(
        vatReturn = VatReturn(
          "9999",
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
        payment = None
      )
    )

    lazy val view = injectedView(vatReturnViewModelFinalReturn)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the pageHeading of 'Final return'" in {
      elementText(Selectors.pageHeading) shouldBe "Submitted return for Cheapo Clothing Final return"
    }
  }
}
