/*
 * Copyright 2019 HM Revenue & Customs
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
import play.twirl.api.Html

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

    val previousReturnsText = "#previous-one"
    val previousReturnsLink = "#previous-one a:nth-of-type(1)"
  }

  "Rendering the Submitted Returns page with a HMRC-MTD-VAT enrolment" when {

    "there is a single return year retrieved" when {

      "there are multiple returns for the year retrieved" should {

        lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
          Seq(
            ReturnObligationsViewModel(
              LocalDate.parse("2018-01-01"),
              LocalDate.parse("2018-03-31"),
              "#001"
            ),
            ReturnObligationsViewModel(
              LocalDate.parse("2018-04-01"),
              LocalDate.parse("2018-06-30"),
              "#002"
            )
          )

        lazy val view: Html = views.html.returns.submittedReturns(
          VatReturnsViewModel(
            returnYears = Seq(2018),
            selectedYear = 2018,
            obligations = exampleReturns,
            hasNonMtdVatEnrolment = false,
            vrn = "999999999"
          )
        )

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
            elementText(Selectors.vatBreadcrumb) shouldBe "Your VAT details"
          }

          "link to the VAT Details page" in {
            element(Selectors.vatBreadcrumbLink).attr("href") shouldBe "vat-details-url"
          }

          "have the text 'Submitted returns'" in {
            elementText(Selectors.submittedReturnsBreadcrumb) shouldBe "Submitted returns"
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
            elementText(Selectors.obligation(1)) shouldBe "View return for the period 1 January to 31 March 2018"
          }

          "contains the correct link to view a specific return" in {
            element(Selectors.obligationLink(1)).attr("href") shouldBe controllers.routes.ReturnsController.vatReturn(2018, "#001").url
          }
        }

        "contain the second return which" should {

          "contains the correct obligation period text" in {
            elementText(Selectors.obligation(2)) shouldBe "View return for the period 1 April to 30 June 2018"
          }

          "contains the correct link to view a specific return" in {
            element(Selectors.obligationLink(2)).attr("href") shouldBe controllers.routes.ReturnsController.vatReturn(2018, "#002").url
          }
        }

        "not contain any tabs" in {
          document.select(Selectors.tabOne) shouldBe empty
        }

      }

      "there is a final return for year of 2018" should {

        val exampleReturn: Seq[ReturnObligationsViewModel] =
          Seq(
            ReturnObligationsViewModel(
              LocalDate.parse("2018-01-01"),
              LocalDate.parse("2018-12-31"),
              mockConfig.finalReturnPeriodKey
            )
          )

        lazy val view: Html = views.html.returns.submittedReturns(
          VatReturnsViewModel(
            returnYears = Seq(2018),
            selectedYear = 2018,
            obligations = exampleReturn,
            hasNonMtdVatEnrolment = false,
            vrn = "999999999"
          )
        )

        lazy implicit val document: Document = Jsoup.parse(view.body)

        "contain the first return which" should {

          "contains the correct obligation period text" in {
            elementText(Selectors.obligation(1)) shouldBe "View return for the period Final return"
          }
        }
      }
    }

    "two return years are retrieved" should {

      val returnYears = Seq(2020, 2019)

      lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
        Seq(
          ReturnObligationsViewModel(
            LocalDate.parse("2020-01-01"),
            LocalDate.parse("2020-03-31"),
            "#001"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2020-04-01"),
            LocalDate.parse("2020-06-30"),
            "#002"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2019-01-01"),
            LocalDate.parse("2019-03-31"),
            "#001"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2019-04-01"),
            LocalDate.parse("2019-06-30"),
            "#002"
          )
        )

      lazy val view = views.html.returns.submittedReturns(
        VatReturnsViewModel(returnYears, 2020, exampleReturns, hasNonMtdVatEnrolment = false, "999999999")
      )

      lazy implicit val document: Document = Jsoup.parse(view.body)

      "have a tab for 2020" should {

        "have the text '2020'" in {
          elementText(Selectors.tabOne) should include("2020")
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabOneHiddenText) shouldBe "Currently viewing returns from 2020"
        }

      }

      "have a tab for 2019" should {

        "have the text '2019'" in {
          elementText(Selectors.tabTwo) should include("2019")
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabTwoHiddenText) shouldBe "View returns from 2019"
        }

        s"contain the correct link to ${controllers.routes.ReturnObligationsController.submittedReturns(2019)}" in {
          element(Selectors.tabTwo).select("a").attr("href") shouldBe
            controllers.routes.ReturnObligationsController.submittedReturns(2019).url
        }

      }

      "not display the previous returns tab" in {
        document.select(Selectors.tabThree).size shouldBe 0
      }

    }

    "there are no returns for the year retrieved" should {

      lazy val view = views.html.returns.submittedReturns(
        VatReturnsViewModel(Seq(2018), 2018, Seq(), hasNonMtdVatEnrolment = false, "999999999")
      )

      lazy implicit val document: Document = Jsoup.parse(view.body)

      "have the correct return heading" in {
        elementText(Selectors.returnsHeading) shouldBe "2018 returns"
      }

      "have the correct alternate content" in {
        elementText(Selectors.noReturnsFound) shouldBe
          "You have not submitted any returns for 2018 yet. You must use accounting software to submit your returns."
      }

    }

  }

  "Rendering the Submitted Returns page with a HMRC-MTD-VAT enrolment and a HMCE-VATDEC-ORG / HMCE-VATVAT-ORG enrolment" when {

    "more than two return years are retrieved" should {

      val returnYears = Seq(2020, 2019, 2018)

      lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
        Seq(
          ReturnObligationsViewModel(
            LocalDate.parse("2020-01-01"),
            LocalDate.parse("2020-03-31"),
            "#001"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2020-04-01"),
            LocalDate.parse("2020-06-30"),
            "#002"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2019-01-01"),
            LocalDate.parse("2019-03-31"),
            "#001"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2019-04-01"),
            LocalDate.parse("2019-06-30"),
            "#002"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2018-01-01"),
            LocalDate.parse("2018-03-31"),
            "#001"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2018-04-01"),
            LocalDate.parse("2018-06-30"),
            "#002"
          )
        )

      lazy val view = views.html.returns.submittedReturns(
        VatReturnsViewModel(returnYears, 2020, exampleReturns, hasNonMtdVatEnrolment = true, "999999999")
      )

      lazy implicit val document: Document = Jsoup.parse(view.body)

      "have a tab for 2020" should {

        "have the text '2020'" in {
          elementText(Selectors.tabOne) should include("2020")
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabOneHiddenText) shouldBe "Currently viewing returns from 2020"
        }

      }

      "have a tab for 2019" should {

        "have the text '2019'" in {
          elementText(Selectors.tabTwo) should include("2019")
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabTwoHiddenText) shouldBe "View returns from 2019"
        }

      }

      "have a tab for 2018" should {

        "have the text '2018'" in {
          elementText(Selectors.tabThree) should include("2018")
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabThreeHiddenText) shouldBe "View returns from 2018"
        }

      }

      "not display the previous returns tab" in {
        document.select(Selectors.tabFour).size shouldBe 0
      }

    }

    "two return years are retrieved" should {

      val returnYears = Seq(2020, 2019)

      lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
        Seq(
          ReturnObligationsViewModel(
            LocalDate.parse("2020-01-01"),
            LocalDate.parse("2020-03-31"),
            "#001"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2020-04-01"),
            LocalDate.parse("2020-06-30"),
            "#002"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2019-01-01"),
            LocalDate.parse("2019-03-31"),
            "#001"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2019-04-01"),
            LocalDate.parse("2019-06-30"),
            "#002"
          )
        )

      lazy val view = views.html.returns.submittedReturns(
        VatReturnsViewModel(returnYears, 2020, exampleReturns, hasNonMtdVatEnrolment = true, "999999999")
      )

      lazy implicit val document: Document = Jsoup.parse(view.body)

      "have a tab for 2020" should {

        "have the text '2020'" in {
          elementText(Selectors.tabOne) should include("2020")
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabOneHiddenText) shouldBe "Currently viewing returns from 2020"
        }

      }

      "have a tab for 2019" should {

        "have the text '2019'" in {
          elementText(Selectors.tabTwo) should include("2019")
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabTwoHiddenText) shouldBe "View returns from 2019"
        }

      }

      "have a tab for previous returns" should {

        "have the text 'Previous Returns'" in {
          elementText(Selectors.tabThree) should include("Previous returns")
        }

        s"contain the correct link to ${controllers.routes.ReturnObligationsController.submittedReturns(2018)}" in {
          element(Selectors.tabThree).select("a").attr("href") shouldBe
            controllers.routes.ReturnObligationsController.submittedReturns(2018).url
        }
      }
    }
  }
}