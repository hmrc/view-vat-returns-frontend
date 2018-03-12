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

import models.viewModels.{ReturnObligationsViewModel, VatReturnsViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class SubmittedReturnsViewSpec extends ViewBaseSpec {

  object Selectors {
    val pageHeading = "h1"
    val btaBreadcrumb = "div.breadcrumbs li:nth-of-type(1)"
    val btaBreadcrumbLink = s"$btaBreadcrumb a"
    val vatBreadcrumb = "div.breadcrumbs li:nth-of-type(2)"
    val vatBreadcrumbLink = s"$vatBreadcrumb a"
    val submittedReturnsBreadcrumb = "div.breadcrumbs li:nth-of-type(3)"
    val submitThroughSoftware = "div > div > p:nth-child(2)"
    val noReturnsFound = ".column-two-thirds p"
    val tabOne = ".tabs-nav li:nth-of-type(1)"
    val tabOneHiddenText = ".tabs-nav li:nth-of-type(1) span"
    val tabTwo = ".tabs-nav li:nth-of-type(2)"
    val tabTwoHiddenText = ".tabs-nav li:nth-of-type(2) span"
    val tabThree = ".tabs-nav li:nth-of-type(3)"
    val tabThreeHiddenText = ".tabs-nav li:nth-of-type(3) span"
    val tabFour = ".tabs-nav li:nth-of-type(4)"
    val tabFourHiddenText = ".tabs-nav li:nth-of-type(4) span"
    val returnsHeading = "h2"
    val period = ".column-two-thirds p"
    def obligation(number: Int): String = s".list-bullet li:nth-of-type($number)"
    def obligationLink(number: Int): String = s".list-bullet li:nth-of-type($number) > a"
    val previousReturnsText = ".column-two-thirds p"
    val previousReturnsLink = "div > div > p > a"
  }

  "Rendering the Submitted Returns page with multiple return years displayed" when {

    val returnYears = Seq(2018, 2017)

    "there are multiple returns for the selected year of 2018" should {

      lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
        Seq(
          ReturnObligationsViewModel(
            LocalDate.parse("2018-01-01"),
            LocalDate.parse("2018-12-31"),
            "#001"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2018-01-01"),
            LocalDate.parse("2018-09-30"),
            "#002"
          )
        )

      lazy val view = views.html.returns.submittedReturns(VatReturnsViewModel(returnYears, 2018, exampleReturns, hasNonMtdVat = false))
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "have the correct return heading" in {
        elementText(Selectors.returnsHeading) shouldBe "2018 returns"
      }

      "have the correct period text" in {
        elementText(Selectors.period) shouldBe "For the period:"
      }

      "contain the first return which" should {

        "contains the correct obligation period text" in {
          elementText(Selectors.obligation(1)) shouldBe "1 January to 31 December 2018"
        }

        "contains the correct link to view a specific return" in {
          element(Selectors.obligationLink(1)).attr("href") shouldBe controllers.routes.ReturnsController.vatReturn(2018, "#001").url
        }
      }

      "contain the second return which" should {

        "contains the correct obligation period text" in {
          elementText(Selectors.obligation(2)) shouldBe "1 January to 30 September 2018"
        }

        "contains the correct link to view a specific return" in {
          element(Selectors.obligationLink(2)).attr("href") shouldBe controllers.routes.ReturnsController.vatReturn(2018, "#002").url
        }
      }
    }

    "there are no returns for the selected year of 2018" should {

      lazy val view = views.html.returns.submittedReturns(VatReturnsViewModel(returnYears, 2018, Seq(), hasNonMtdVat = false))
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "have the correct document title" in {
        document.title shouldBe "Submitted returns"
      }

      "have the correct page heading" in {
        elementText(Selectors.pageHeading) shouldBe "Submitted returns"
      }

      "render breadcrumbs which" should {

        "have the text 'Business tax account'" in {
          elementText(Selectors.btaBreadcrumb) shouldBe "Business tax account"
        }

        "link to BTA" in {
          element(Selectors.btaBreadcrumbLink).attr("href") shouldBe "bta-url"
        }

        "have the text 'VAT'" in {
          elementText(Selectors.vatBreadcrumb) shouldBe "VAT"
        }

        "link to the VAT Details page" in {
          element(Selectors.vatBreadcrumbLink).attr("href") shouldBe "vat-details-url"
        }

        "have the text 'Submitted returns'" in {
          elementText(Selectors.submittedReturnsBreadcrumb) shouldBe "Submitted returns"
        }
      }

      "have tabs for each return year" should {

        "tab one" should {

          "have the text '2018'" in {
            elementText(Selectors.tabOne) should include("2018")
          }

          "contain visually hidden text" in {
            elementText(Selectors.tabOneHiddenText) shouldBe "Currently viewing returns from 2018"
          }
        }

        "tab two" should {

          "have the text '2017'" in {
            elementText(Selectors.tabTwo) should include("2017")
          }

          s"contain a link to ${controllers.routes.ReturnObligationsController.submittedReturns(2017).url}" in {
            element(Selectors.tabTwo).select("a").attr("href") shouldBe
              controllers.routes.ReturnObligationsController.submittedReturns(2017).url
          }

          "contain visually hidden text" in {
            elementText(Selectors.tabTwoHiddenText) shouldBe "View returns from 2017"
          }
        }
      }

      "have the correct return heading" in {
        elementText(Selectors.returnsHeading) shouldBe "2018 returns"
      }

      "have the correct alternate content when no returns are found" in {
        elementText(Selectors.noReturnsFound) shouldBe
          "You haven’t submitted any returns for 2018 yet. You must use accounting software to submit your returns."
      }
    }
  }

  "Rendering the VAT Returns page with only one return year displayed" when {

    lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
      Seq(
        ReturnObligationsViewModel(
          LocalDate.parse("2018-01-01"),
          LocalDate.parse("2018-12-31"),
          "#001"
        ),
        ReturnObligationsViewModel(
          LocalDate.parse("2018-01-01"),
          LocalDate.parse("2018-09-30"),
          "#002"
        )
      )

    "the first tab is selected and the user has an old VAT enrolment" should {
      lazy val view = views.html.returns.submittedReturns(VatReturnsViewModel(Seq[Int](2018), 2018, exampleReturns, hasNonMtdVat = true))
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "have a tab for the returned year " should {

        "have the text '2018'" in {
          elementText(Selectors.tabOne) should include("2018")
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabOneHiddenText) shouldBe "Currently viewing returns from 2018"
        }
      }

      "have a tab for previous returns" should {

        "have the text 'Previous returns" in {
          elementText(Selectors.tabTwo) should include("Previous returns")
        }

        s"contain a link to ${controllers.routes.ReturnObligationsController.submittedReturns(2017).url}" in {
          element(Selectors.tabTwo).select("a").attr("href") shouldBe controllers.routes.ReturnObligationsController.submittedReturns(2017).url
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabTwoHiddenText) shouldBe "View previous returns"
        }
      }

      "have the correct return heading" in {
        elementText(Selectors.returnsHeading) shouldBe "2018 returns"
      }

      "have the correct period text" in {
        elementText(Selectors.period) shouldBe "For the period:"
      }

      "contain the first return which" should {

        "contains the correct obligation period text" in {
          elementText(Selectors.obligation(1)) shouldBe "1 January to 31 December 2018"
        }

        "contains the correct link to view a specific return" in {
          element(Selectors.obligationLink(1)).attr("href") shouldBe controllers.routes.ReturnsController.vatReturn(2018, "#001").url
        }
      }

      "contain the second return which" should {

        "contains the correct obligation period text" in {
          elementText(Selectors.obligation(2)) shouldBe "1 January to 30 September 2018"
        }

        "contains the correct link to view a specific return" in {
          element(Selectors.obligationLink(2)).attr("href") shouldBe controllers.routes.ReturnsController.vatReturn(2018, "#002").url
        }
      }
    }

    "the second tab is selected and the user has an old VAT enrolment" should {

      lazy val view = views.html.returns.submittedReturns(VatReturnsViewModel(Seq[Int](2018), 2017, exampleReturns, hasNonMtdVat = true))
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "have a tab for the returned year " should {

        "have the text '2018'" in {
          elementText(Selectors.tabOne) should include("2018")
        }

        s"contain a link to ${controllers.routes.ReturnObligationsController.submittedReturns(2018).url}" in {
          element(Selectors.tabOne).select("a").attr("href") shouldBe controllers.routes.ReturnObligationsController.submittedReturns(2018).url
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabOneHiddenText) shouldBe "View returns from 2018"
        }
      }

      "have a tab for previous returns" should {

        "have the text 'Previous returns" in {
          elementText(Selectors.tabTwo) should include("Previous returns")
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabTwoHiddenText) shouldBe "Currently viewing previous returns"
        }
      }

      "have the correct return heading" in {
        elementText(Selectors.returnsHeading) shouldBe "Previous returns"
      }

      "have the correct information text under the heading" in {
        elementText(Selectors.previousReturnsText) shouldBe
          "If you've submitted returns without using the software for this new service, you can view you previous returns (opens in new tab)."
      }

      "contains the link" in {
        element(Selectors.previousReturnsLink).attr("href") shouldBe ""
      }
    }

    "the user has no old VAT enrolment" should {

      lazy val view = views.html.returns.submittedReturns(VatReturnsViewModel(Seq[Int](2018), 2018, exampleReturns, hasNonMtdVat = false))
      lazy val document: Document = Jsoup.parse(view.body)

      "not show the Previous Returns tab" in {
        document.select(Selectors.tabTwo) shouldBe empty
      }
    }
  }
}
