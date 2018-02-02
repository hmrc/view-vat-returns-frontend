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

import models.{VatReturnObligation, VatReturnObligations}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class CompletedReturnsViewSpec extends ViewBaseSpec {

  object Selectors {
    val pageHeading = "h1"
    val submitThroughSoftware = "div > div > p:nth-child(2)"
    val noReturnsFound = "div > div > section > p"
    val tabOne = ".tabs-nav li:nth-of-type(1)"
    val tabOneHiddenText = ".tabs-nav li:nth-of-type(1) span"
    val tabTwo = ".tabs-nav li:nth-of-type(2)"
    val tabTwoHiddenText = ".tabs-nav li:nth-of-type(2) span"
    val tabThree = ".tabs-nav li:nth-of-type(3)"
    val tabThreeHiddenText = ".tabs-nav li:nth-of-type(3) span"
    val tabFour = ".tabs-nav li:nth-of-type(4)"
    val tabFourHiddenText = ".tabs-nav li:nth-of-type(4) span"
    val returnsHeading = ".divider--bottom h2"
    val period = ".divider--bottom p"
    def obligation(number: Int): String = s".divider--bottom .list-bullet li:nth-of-type($number)"
    def obligationLink(number: Int): String = s".divider--bottom .list-bullet li:nth-of-type($number) > a"
    val otherReturns = "h3"
    val otherReturnsGuidance = "div > div > p:nth-child(6)"
    val otherReturnsLink = "div > div > p:nth-child(6) > a"
  }

  val returnYears = Seq(2018, 2017, 2016, 2015)

  "Rendering the VAT Returns page with no returns for the selected year of 2018" should {

    lazy val view = views.html.returns.completedReturns(VatReturnObligations(Seq()), returnYears, 2018)
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

    "have tabs for each return year which" should {

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

        s"contain a link to ${controllers.routes.ReturnObligationsController.completedReturns(2017).url}" in {
          element(Selectors.tabTwo).select("a").attr("href") shouldBe controllers.routes.ReturnObligationsController.completedReturns(2017).url
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabTwoHiddenText) shouldBe "View returns from 2017"
        }
      }

      "tab three" should {

        "have the text '2016'" in {
          elementText(Selectors.tabThree) should include("2016")
        }

        s"contain a link to ${controllers.routes.ReturnObligationsController.completedReturns(2016).url}" in {
          element(Selectors.tabThree).select("a").attr("href") shouldBe controllers.routes.ReturnObligationsController.completedReturns(2016).url
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabThreeHiddenText) shouldBe "View returns from 2016"
        }
      }

      "tab four" should {

        "have the text '2015'" in {
          elementText(Selectors.tabFour) should include("2015")
        }

        s"contain a link to ${controllers.routes.ReturnObligationsController.completedReturns(2015).url}" in {
          element(Selectors.tabFour).select("a").attr("href") shouldBe controllers.routes.ReturnObligationsController.completedReturns(2015).url
        }

        "contain visually hidden text" in {
          elementText(Selectors.tabFourHiddenText) shouldBe "View returns from 2015"
        }
      }
    }

    "have the correct return heading" in {
      elementText(Selectors.returnsHeading) shouldBe "2018 Returns"
    }

    "have the correct alternate content for when no returns are found" in {
      // TODO: this will change next iteration
      elementText(Selectors.noReturnsFound) shouldBe "Alternate Content"
    }

    "the other returns guidance" should {

      "have the correct title" in {
        elementText(Selectors.otherReturns) shouldBe "Other returns"
      }

      "contain the correct text" in {
        elementText(Selectors.otherReturnsGuidance) shouldBe "You can also view returns submitted before using accounting software (opens in new tab)."
      }

      "contain a link to ''" in {
        element(Selectors.otherReturnsLink).attr("href") shouldBe ""
      }
    }
  }

  "Rendering the VAT Returns page with multiple returns for the selected year of 2018" should {

    lazy val exampleReturns: VatReturnObligations = VatReturnObligations(
      Seq(
        VatReturnObligation(
          LocalDate.parse("2017-01-01"),
          LocalDate.parse("2017-12-31"),
          LocalDate.parse("2018-01-31"),
          "O",
          None,
          "#001"
        ),
        VatReturnObligation(
          LocalDate.parse("2017-01-01"),
          LocalDate.parse("2017-09-30"),
          LocalDate.parse("2018-10-31"),
          "O",
          None,
          "#001"
        )
      )
    )

    lazy val view = views.html.returns.completedReturns(exampleReturns, returnYears, 2018)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct return heading" in {
      elementText(Selectors.returnsHeading) shouldBe "2018 Returns"
    }

    "have the correct period text" in {
      elementText(Selectors.period) shouldBe "For the period:"
    }

    "contain the first return which" should {

      "contains the correct obligation period text" in {
        elementText(Selectors.obligation(1)) shouldBe "1 January to 31 December 2017"
      }

      "contains the correct link to view a specific return" in {
        element(Selectors.obligationLink(1)).attr("href") shouldBe controllers.routes.ReturnsController.vatReturnDetails("2017-01-01", "2017-12-31").url
      }
    }

    "contain the second return which" should {

      "contains the correct obligation period text" in {
        elementText(Selectors.obligation(2)) shouldBe "1 January to 30 September 2017"
      }

      "contains the correct link to view a specific return" in {
        element(Selectors.obligationLink(2)).attr("href") shouldBe controllers.routes.ReturnsController.vatReturnDetails("2017-01-01", "2017-09-30").url
      }
    }
  }
}
