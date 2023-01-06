/*
 * Copyright 2023 HM Revenue & Customs
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
import views.html.returns.ReturnDeadlinesView

class ReturnDeadlinesViewSpec extends ViewBaseSpec {

  val injectedView: ReturnDeadlinesView = inject[ReturnDeadlinesView]

  object Selectors {
    val pageHeading = "#content h1"
    val submitThroughSoftware = "#content > p.govuk-body"
    val howToDoThis = "details > summary > span"
    val downloadSoftware = ".govuk-list--number li:nth-child(1)"
    val vatRecords = ".govuk-list--number li:nth-child(2)"
    val sendReturns = ".govuk-list--number li:nth-child(3)"

    val firstDeadlineDueDate = ".govuk-list li:nth-of-type(1)"
    val firstDeadlinePeriod = ".govuk-list li:nth-of-type(1) .govuk-hint"
    val secondDeadlineDueDate = ".govuk-list li:nth-of-type(2)"
    val secondDeadlinePeriod = ".govuk-list li:nth-of-type(2) .govuk-hint"

    val btaBreadcrumb = "div.govuk-breadcrumbs li:nth-of-type(1)"
    val btaBreadCrumbLink = "div.govuk-breadcrumbs li:nth-of-type(1) a"
    val vatDetailsBreadCrumb = "div.govuk-breadcrumbs li:nth-of-type(2)"
    val vatDetailsBreadcrumbLink = "div.govuk-breadcrumbs li:nth-of-type(2) a"

    val overdueLabel = ".govuk-tag--red"
    val caption = "#content > span"
    val backLink = "body > div.govuk-width-container > a"
  }

  "The Return deadlines page for principal user" should {

    "Render the Return deadlines page with a single deadline" should {

      val singleDeadline = Seq(
        ReturnDeadlineViewModel(
          LocalDate.parse("2018-02-02"),
          LocalDate.parse("2018-01-01"),
          LocalDate.parse("2018-01-01"),
          periodKey = "18CC"
        )
      )

      lazy val view = injectedView(singleDeadline, Html(""), None)
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

      "have the correct document title" in {
        document.title shouldBe "Return deadlines - Manage your VAT account - GOV.UK"
      }

      "have the correct page heading" in {
        elementText(Selectors.pageHeading) shouldBe "Return deadlines"
      }

      "have the correct message regarding submitting returns through software" in {
        elementText(Selectors.submitThroughSoftware) shouldBe "Use your accounting software to submit a return by:"
      }

      "have the correct obligation due date" in {
        elementText(Selectors.firstDeadlineDueDate) should include("2 February 2018")
      }

      "have the correct obligation start and end date text" in {
        elementText(Selectors.firstDeadlinePeriod) shouldBe "for the period 1 January to 1 January 2018"
      }

      "have the correct hint box title" in {
        elementText(Selectors.howToDoThis) shouldBe "How to submit a return"
      }

      "have the correct message regarding downloading software in the hint box" in {
        elementText(Selectors.downloadSoftware) shouldBe "Choose accounting software that supports this service (opens in a new tab) if you have not already."
      }

      "have a link to govuk commercial software page" in {
        document.select(s"${Selectors.downloadSoftware} > a").first().attr("href") shouldBe "https://www.gov.uk/guidance/software-for-sending-income-tax-updates"
      }

      "have the correct message regarding VAT records in the hint box" in {
        elementText(Selectors.vatRecords) shouldBe "Keep your VAT records in your accounting software."
      }

      "have the correct message regarding sending HMRC VAT returns in the hint box" in {
        elementText(Selectors.sendReturns) shouldBe "Submit any VAT Returns before your deadlines."
      }

      "do not have business entity name"in {
        elementExtinct(Selectors.caption)
      }
    }

    "Render the Return deadlines page with multiple deadlines" should {

      val multipleDeadlines = Seq(
        ReturnDeadlineViewModel(
          LocalDate.parse("2018-02-02"),
          LocalDate.parse("2018-01-01"),
          LocalDate.parse("2018-01-01"),
          periodKey = "18CC"
        ),
        ReturnDeadlineViewModel(
          LocalDate.parse("2018-10-12"),
          LocalDate.parse("2018-09-07"),
          LocalDate.parse("2018-09-07"),
          overdue = true,
          periodKey = "18CC"
        )
      )

      lazy val view = injectedView(multipleDeadlines, Html(""), None)
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "have the correct obligation due date for the first deadline" in {
        elementText(Selectors.firstDeadlineDueDate) should include("2 February 2018")
      }

      "have the correct obligation start and end date text for the first deadline" in {
        elementText(Selectors.firstDeadlinePeriod) shouldBe "for the period 1 January to 1 January 2018"
      }

      "have the correct obligation due date for the second deadline" in {
        elementText(Selectors.secondDeadlineDueDate) should include("12 October 2018")
      }

      "have the correct obligation start and end date text for the second deadline" in {
        elementText(Selectors.secondDeadlinePeriod) shouldBe "for the period 7 September to 7 September 2018"
      }

      "have the overdue label" in {
        elementText(Selectors.overdueLabel) shouldBe "overdue"
      }

      "do not have business entity name"in {
        elementExtinct(Selectors.caption)
      }
    }

    "Render the Return deadlines page with a final return" should {

      val finalReturnDeadline = Seq(
        ReturnDeadlineViewModel(
          LocalDate.parse("2018-02-02"),
          LocalDate.parse("2018-01-01"),
          LocalDate.parse("2018-01-01"),
          periodKey = mockConfig.finalReturnPeriodKey
        )
      )

      lazy val view = injectedView(finalReturnDeadline, Html(""), None)
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "have the correct obligation due date for the deadline" in {
        elementText(Selectors.firstDeadlineDueDate) should include("2 February 2018")
      }

      "have the wording for the final return period" in {
        elementText(Selectors.firstDeadlinePeriod) shouldBe "for your final return"
      }

      "do not have business entity name"in {
        elementExtinct(Selectors.caption)
      }
      "not have an overdue label" in {
        elementExtinct(".govuk-tag--red")
      }
    }
  }

  "The Return deadlines page for agent user" should {

    val singleDeadline = Seq(
      ReturnDeadlineViewModel(
        LocalDate.parse("2018-02-02"),
        LocalDate.parse("2018-01-01"),
        LocalDate.parse("2018-01-01"),
        periodKey = "18CC"
      )
    )
    implicit val user: User = agentUser
    lazy val view = injectedView(singleDeadline, Html(""), Some("Ancient Antiques"))
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

    "have the correct message regarding submitting returns through software" in {
      elementText(Selectors.submitThroughSoftware) shouldBe "Use your accounting software to submit a return by:"
    }

    "have the correct obligation due date" in {
      elementText(Selectors.firstDeadlineDueDate) should include("2 February 2018")
    }

    "have the correct obligation start and end date text" in {
      elementText(Selectors.firstDeadlinePeriod) shouldBe "for the period 1 January to 1 January 2018"
    }

    "have the correct hint box title" in {
      elementText(Selectors.howToDoThis) shouldBe "How to submit a return"
    }

    "have the correct message regarding downloading software in the hint box" in {
      elementText(Selectors.downloadSoftware) shouldBe "Choose accounting software that supports this service (opens in a new tab) if you have not already."
    }

    "have a link to govuk commercial software page" in {
      document.select(s"${Selectors.downloadSoftware} > a").first().attr("href") shouldBe "https://www.gov.uk/guidance/software-for-sending-income-tax-updates"
    }

    "have the correct message regarding VAT records in the hint box" in {
      elementText(Selectors.vatRecords) shouldBe "Keep your VAT records in your accounting software."
    }

    "have the correct message regarding sending HMRC VAT returns in the hint box" in {
      elementText(Selectors.sendReturns) shouldBe "Submit any VAT Returns before your deadlines."
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
