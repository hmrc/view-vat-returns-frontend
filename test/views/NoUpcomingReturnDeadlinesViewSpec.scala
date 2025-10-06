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

import models.User

import java.time.LocalDate
import models.viewModels.ReturnDeadlineViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import views.html.returns.NoUpcomingReturnDeadlinesView

class NoUpcomingReturnDeadlinesViewSpec extends ViewBaseSpec {

  val injectedView: NoUpcomingReturnDeadlinesView = inject[NoUpcomingReturnDeadlinesView]

  object Selectors {
    val pageHeading = "#content h1"

    val btaBreadcrumb = ".govuk-breadcrumbs li:nth-of-type(1)"
    val btaBreadCrumbLink = ".govuk-breadcrumbs li:nth-of-type(1) a"
    val vatDetailsBreadCrumb = ".govuk-breadcrumbs li:nth-of-type(2)"
    val vatDetailsBreadcrumbLink = ".govuk-breadcrumbs li:nth-of-type(2) a"

    val noReturnsNextDeadline = "#no-returns-next-deadline"
    val noReturnsDue = "#no-returns"
    val caption = "#content > span"
    val backLink = "body > div.govuk-width-container > a"

  }

  "The Return deadlines page for a principal user" should {

    "Render the Return deadlines page with no fulfilled obligations" should {

      val noFulfilledObligation = None
      lazy val view = injectedView(noFulfilledObligation, Html(""), None, "")
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "render the breadcrumbs which" should {

        "have the 'Business tax account' title" in {
          elementText(Selectors.btaBreadcrumb) shouldBe "Business tax account"
        }

        "and links to the BTA service" in {
          element(Selectors.btaBreadCrumbLink).attr("href") shouldBe "bta-url"
        }

        "have the 'VAT' title" in {
          elementText(Selectors.vatDetailsBreadCrumb) shouldBe "Your VAT account"
        }

        "and links to the VAT Summary service" in {
          element(Selectors.vatDetailsBreadcrumbLink).attr("href") shouldBe "vat-details-url"
        }

      }

      "have the correct text for no deadlines with guidance" in {
        elementText(Selectors.noReturnsDue) shouldBe
          "You do not have any returns due right now. Your next deadline will show here on the first day of your next" +
            " accounting period."
      }

      "do not have business entity name"in {
        elementExtinct(Selectors.caption)
      }
    }

    "Rendering the Return deadlines page with a fulfilled obligation" should {

      val fulfilledObligation = Some(ReturnDeadlineViewModel(
        periodTo = LocalDate.parse("2018-04-01"),
        periodFrom = LocalDate.parse("2018-01-01"),
        due = LocalDate.parse("2018-05-01"),
        periodKey = "18CC"
      ))
      lazy val view = injectedView(fulfilledObligation, Html(""), None, "Non MTDfB")
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "have the correct text for no deadlines" in {
        elementText(Selectors.noReturnsNextDeadline) shouldBe
          "We received your return for the period 1 January to 1 April 2018."
      }

      "have the correct text for no deadlines using non breaking space" in {
        elementText(Selectors.noReturnsNextDeadline)
          .contains("We received your return for the period 1\u00a0January to 1\u00a0April\u00a02018.")
      }

      "have the correct received return guidance" in {
        elementText(Selectors.noReturnsDue) shouldBe
          "You do not have any returns due right now. Your next deadline will show here on the first day of your next accounting period."
      }

      "do not have business entity name"in {
        elementExtinct(Selectors.caption)
      }
    }
  }

  "The Return deadlines page for agent user" should {

    val fulfilledObligation = Some(ReturnDeadlineViewModel(
      periodTo = LocalDate.parse("2018-04-01"),
      periodFrom = LocalDate.parse("2018-01-01"),
      due = LocalDate.parse("2018-05-01"),
      periodKey = "18CC"
    ))

    implicit val user: User = agentUser
    lazy val view = injectedView(fulfilledObligation, Html(""), Some("Ancient Antiques"), "")
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct title" in {
      document.title shouldBe "Return deadlines - Your client’s VAT details - GOV.UK"
    }

    "have the correct page heading" in {
      elementText(Selectors.pageHeading) shouldBe "Return deadlines"
    }

    "have the client name caption" in {
      elementText(Selectors.caption) shouldBe "Ancient Antiques"
    }

    "have the correct text for no deadlines" in {
      elementText(Selectors.noReturnsNextDeadline) shouldBe
        "We received your return for the period 1 January to 1 April 2018."
    }

    "have the correct text for no deadlines using non breaking space" in {
      elementText(Selectors.noReturnsNextDeadline)
        .contains("We received your return for the period 1\u00a0January to 1\u00a0April\u00a02018.")
    }

    "have the correct received return guidance" in {
      elementText(Selectors.noReturnsDue) shouldBe
        "You do not have any returns due right now. Your next deadline will show here on the first day of your next accounting period."
    }

    "renders a back link" which {

      "has the correct text" in {
        elementText(Selectors.backLink) shouldBe "Back"
      }

      "has the correct href" in {
        element(Selectors.backLink).attr("href") shouldBe "agent-client-agent-action"
      }
    }
  }

  }
