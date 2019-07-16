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

import models.User
import models.viewModels.ReturnDeadlineViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.exceptions.TestFailedException
import play.api.i18n.Lang

class OptOutReturnDeadlinesViewSpec extends ViewBaseSpec {

  object Selectors {
    val pageHeading = "#content h1"
    val submitThroughSoftware = "#content > article > div > div > p"
    val howToDoThis = "details > summary > span"
    val downloadSoftware = ".list-number li:nth-child(1)"
    val vatRecords = ".list-number li:nth-child(2)"
    val sendReturns = ".list-number li:nth-child(3)"

    val firstDeadlineDueDate = ".list li:nth-of-type(1)"
    val firstDeadlinePeriod = ".list li:nth-of-type(1) .form-hint"
    val secondDeadlineDueDate = ".list li:nth-of-type(2)"
    val secondDeadlinePeriod = ".list li:nth-of-type(2) .form-hint"

    val btaBreadcrumb = "div.breadcrumbs li:nth-of-type(1)"
    val btaBreadCrumbLink = "div.breadcrumbs li:nth-of-type(1) a"
    val vatDetailsBreadCrumb = "div.breadcrumbs li:nth-of-type(2)"
    val vatDetailsBreadcrumbLink = "div.breadcrumbs li:nth-of-type(2) a"
    val returnDeadlinesBreadCrumb = "div.breadcrumbs li:nth-of-type(3)"

    val backLink = "#link-back"

    val overdueLabel = ".task-overdue"
    val cannotSubmitText = "li > p > span:nth-child(3)"
  }

  "Rendering the Opted-Out Return deadlines page with a single deadline" when {

    "end date has passed" should {

      val singleDeadline = Seq(
        ReturnDeadlineViewModel(
          LocalDate.parse("2018-02-02"),
          LocalDate.parse("2018-01-01"),
          end = LocalDate.parse("2018-01-01"),
          periodKey = "18CC"
        )
      )

      val currentDate = LocalDate.parse("2018-01-02")

      lazy val view = views.html.returns.optOutReturnDeadlines(singleDeadline, currentDate)
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

        "have the 'Submit VAT Return' title" in {
          elementText(Selectors.returnDeadlinesBreadCrumb) shouldBe "Submit VAT Return"
        }
      }

      "not render the back link" in {
        an[TestFailedException] should be thrownBy element(Selectors.backLink)
      }

      "have the correct document title" in {
        document.title shouldBe "Submit VAT Return"
      }

      "have the correct page heading" in {
        elementText(Selectors.pageHeading) shouldBe "Submit VAT Return"
      }

      "have the correct obligation due date" in {
        elementText(Selectors.firstDeadlineDueDate) should include("2 February 2018")
      }

      "have the correct obligation start and end date text" in {
        elementText(Selectors.firstDeadlinePeriod) shouldBe "for the period 1 January to 1 January 2018"
      }

      "have a submit-your-return link" in {
        document.getElementById("submit-return-link").text() shouldBe "Submit VAT Return"
      }

      "have the correct GA event tag for submitting a return" in {
        document.getElementById("submit-return-link").attr("data-journey-click") shouldBe "submit-return:click:return-deadlines"
      }

      "have the correct GA event tag for a page view" in {
        document.getElementsByTag("h1").attr("data-metrics") shouldBe "opted-out:view:return-deadlines"
      }
    }

    "end date has not yet passed" should {

      val singleDeadline = Seq(
        ReturnDeadlineViewModel(
          LocalDate.parse("2018-02-02"),
          LocalDate.parse("2018-01-01"),
          end = LocalDate.parse("2018-12-31"),
          periodKey = "18CC"
        )
      )

      val currentDate = LocalDate.parse("2018-12-30")

      lazy val view = views.html.returns.optOutReturnDeadlines(singleDeadline, currentDate)
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "show text regarding when return can be submitted" in {
        document.select(Selectors.cannotSubmitText).text() shouldBe "You will be able to submit your return from the 1 January."
      }

      "not show a 'Submit VAT Return' link" in {
        document.getElementById("submit-return-link") shouldBe null
      }
    }

    "end date is today" should {

      val currentDate = LocalDate.parse("2018-12-31")

      val singleDeadline = Seq(
        ReturnDeadlineViewModel(
          LocalDate.parse("2018-02-02"),
          LocalDate.parse("2018-01-01"),
          end = currentDate,
          periodKey = "18CC"
        )
      )

      lazy val view = views.html.returns.optOutReturnDeadlines(singleDeadline, currentDate)
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "show text regarding when return can be submitted" in {
        document.select(Selectors.cannotSubmitText).text() shouldBe "You will be able to submit your return from the 1 January."
      }

      "not show a 'Submit VAT Return' link" in {
        document.getElementById("submit-return-link") shouldBe null
      }
    }

    "user is an agent" should {

      val singleDeadline = Seq(
        ReturnDeadlineViewModel(
          LocalDate.parse("2018-02-02"),
          LocalDate.parse("2018-01-01"),
          end = LocalDate.parse("2018-01-01"),
          periodKey = "18CC"
        )
      )

      val currentDate = LocalDate.parse("2018-01-02")

      lazy val view = views.html.returns.optOutReturnDeadlines(singleDeadline, currentDate)(request,
        messages,
        mockConfig,
        messages.lang,
        User("", arn = Some(""))
      )

      lazy implicit val document: Document = Jsoup.parse(view.body)

      "have the correct GA event tag for submitting a return" in {
        document.getElementById("submit-return-link").attr("data-journey-click") shouldBe "agent_submit-return:click:return-deadlines"
      }

      "have the correct GA event tag for a page view" in {
        document.getElementsByTag("h1").attr("data-metrics") shouldBe "agent_opted-out:view:return-deadlines"
      }
    }
  }

  "Rendering the Opted-Out Return deadlines page with a single deadline for an agent" when {

    "end date has passed" should {

      val singleDeadline = Seq(
        ReturnDeadlineViewModel(
          LocalDate.parse("2018-02-02"),
          LocalDate.parse("2018-01-01"),
          end = LocalDate.parse("2018-01-01"),
          periodKey = "18CC"
        )
      )

      val currentDate = LocalDate.parse("2018-01-02")

      lazy val view = views.html.returns.optOutReturnDeadlines(singleDeadline, currentDate)(
        fakeRequestWithClientsVRN, messages, mockConfig, Lang.apply("en"), agentUser
      )
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "do not render the breadcrumbs which" in {
        an[TestFailedException] should be thrownBy elementText(Selectors.btaBreadcrumb)
        an[TestFailedException] should be thrownBy element(Selectors.btaBreadCrumbLink)
        an[TestFailedException] should be thrownBy elementText(Selectors.vatDetailsBreadCrumb)
        an[TestFailedException] should be thrownBy element(Selectors.vatDetailsBreadcrumbLink)
      }

      "render back link" in {
        elementText(Selectors.backLink) shouldBe "Back"
      }

      "have the correct obligation due date" in {
        elementText(Selectors.firstDeadlineDueDate) should include("2 February 2018")
      }

      "have the correct obligation start and end date text" in {
        elementText(Selectors.firstDeadlinePeriod) shouldBe "for the period 1 January to 1 January 2018"
      }

      "have a submit-your-return link" in {
        document.getElementById("submit-return-link").text() shouldBe "Submit VAT Return"
      }

      "have the correct GA event tag for submitting a return" in {
        document.getElementById("submit-return-link").attr("data-journey-click") shouldBe "agent_submit-return:click:return-deadlines"
      }

      "have the correct GA event tag for a page view" in {
        document.getElementsByTag("h1").attr("data-metrics") shouldBe "agent_opted-out:view:return-deadlines"
      }
    }

    "end date has not yet passed" should {

      val singleDeadline = Seq(
        ReturnDeadlineViewModel(
          LocalDate.parse("2018-02-02"),
          LocalDate.parse("2018-01-01"),
          end = LocalDate.parse("2018-12-31"),
          periodKey = "18CC"
        )
      )

      val currentDate = LocalDate.parse("2018-12-30")

      lazy val view = views.html.returns.optOutReturnDeadlines(singleDeadline, currentDate)
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "show text regarding when return can be submitted" in {
        document.select(Selectors.cannotSubmitText).text() shouldBe "You will be able to submit your return from the 1 January."
      }

      "not show a 'Submit VAT Return' link" in {
        document.getElementById("submit-return-link") shouldBe null
      }
    }

    "end date is today" should {

      val currentDate = LocalDate.parse("2018-12-31")

      val singleDeadline = Seq(
        ReturnDeadlineViewModel(
          LocalDate.parse("2018-02-02"),
          LocalDate.parse("2018-01-01"),
          end = currentDate,
          periodKey = "18CC"
        )
      )

      lazy val view = views.html.returns.optOutReturnDeadlines(singleDeadline, currentDate)
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "show text regarding when return can be submitted" in {
        document.select(Selectors.cannotSubmitText).text() shouldBe "You will be able to submit your return from the 1 January."
      }

      "not show a 'Submit VAT Return' link" in {
        document.getElementById("submit-return-link") shouldBe null
      }
    }

    "user is an agent" should {

      val singleDeadline = Seq(
        ReturnDeadlineViewModel(
          LocalDate.parse("2018-02-02"),
          LocalDate.parse("2018-01-01"),
          end = LocalDate.parse("2018-01-01"),
          periodKey = "18CC"
        )
      )

      val currentDate = LocalDate.parse("2018-01-02")

      lazy val view = views.html.returns.optOutReturnDeadlines(singleDeadline, currentDate)(request,
        messages,
        mockConfig,
        messages.lang,
        User("", arn = Some(""))
      )

    }
  }

  "Rendering the Opted-Out Return deadlines page with multiple deadlines" should {

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

    val currentDate = LocalDate.parse("2018-01-02")

    lazy val view = views.html.returns.optOutReturnDeadlines(multipleDeadlines, currentDate)

    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct obligation due date for the first deadline" in {
      elementText(Selectors.firstDeadlineDueDate) should include("Return deadline 2 February 2018")
    }

    "have the correct obligation start and end date text for the first deadline" in {
      elementText(Selectors.firstDeadlinePeriod) shouldBe "for the period 1 January to 1 January 2018"
    }

    "have a submit-your-return link" in {
      document.getElementById("submit-return-link").text() shouldBe "Submit VAT Return"
    }

    "have the correct obligation due date for the second deadline" in {
      elementText(Selectors.secondDeadlineDueDate) should include("Return deadline 12 October 2018")
    }

    "have the correct obligation start and end date text for the second deadline" in {
      elementText(Selectors.secondDeadlinePeriod) shouldBe "for the period 7 September to 7 September 2018"
    }

    "have the overdue label" in {
      elementText(Selectors.overdueLabel) shouldBe "overdue"
    }
  }

  "Rendering the Opted-Out Return deadlines page with a final return" should {

    val finalReturnDeadline = Seq(
      ReturnDeadlineViewModel(
        LocalDate.parse("2018-02-02"),
        LocalDate.parse("2018-01-01"),
        LocalDate.parse("2018-01-01"),
        periodKey = mockConfig.finalReturnPeriodKey
      )
    )

    val currentDate = LocalDate.parse("2018-01-02")

    lazy val view = views.html.returns.optOutReturnDeadlines(finalReturnDeadline, currentDate)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct obligation due date for the deadline" in {
      elementText(Selectors.firstDeadlineDueDate) should include("Return deadline 2 February 2018")
    }

    "have the wording for the final return period" in {
      elementText(Selectors.firstDeadlinePeriod) shouldBe "for your final return"
    }
  }
}
