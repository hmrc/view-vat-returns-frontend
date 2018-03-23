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

import models.viewModels.ReturnDeadlineViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ReturnDeadlinesViewSpec extends ViewBaseSpec {

  val userVrn = "555555555"

  object Selectors {
    val pageHeading = "#content h1"
    val submitThroughSoftware = "#content > article > div > div > p"
    val howToDoThis = "details > summary > span"
    val downloadSoftware = ".list-number li:nth-child(1)"
    val vatRecords = ".list-number li:nth-child(2)"
    val sendReturns = ".list-number li:nth-child(3)"

    val firstDeadlineDueDate = ".list-bullet li:nth-of-type(1)"
    val firstDeadlinePeriod = ".list-bullet li:nth-of-type(1) .form-hint"
    val secondDeadlineDueDate = ".list-bullet li:nth-of-type(2)"
    val secondDeadlinePeriod = ".list-bullet li:nth-of-type(2) .form-hint"

    val btaBreadcrumb = "div.breadcrumbs li:nth-of-type(1)"
    val btaBreadCrumbLink = "div.breadcrumbs li:nth-of-type(1) a"
    val vatDetailsBreadCrumb = "div.breadcrumbs li:nth-of-type(2)"
    val vatDetailsBreadcrumbLink = "div.breadcrumbs li:nth-of-type(2) a"
    val returnDeadlinesBreadCrumb = "div.breadcrumbs li:nth-of-type(3)"

    val overdueLabel = ".task-overdue"
    val noReturnsNextDeadline = "#content > article > div > div > p:nth-of-type(1)"
    val noReturnsDue = "#content > article > div > div > p:nth-of-type(2)"
  }

  "Rendering the Return deadlines page with a single deadline" should {

    val singleDeadline = Seq(
      ReturnDeadlineViewModel(
        LocalDate.parse("2018-02-02"),
        LocalDate.parse("2018-01-01"),
        LocalDate.parse("2018-01-01"))
    )

    lazy val view = views.html.returns.returnDeadlines(singleDeadline, userVrn)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "render the breadcrumbs which" should {

      "have the 'Business tax account' title" in {
        elementText(Selectors.btaBreadcrumb) shouldBe "Business tax account"
      }

      "and links to the BTA service" in {
        element(Selectors.btaBreadCrumbLink).attr("href") shouldBe "bta-url"
      }

      "have the 'VAT' title" in {
        elementText(Selectors.vatDetailsBreadCrumb) shouldBe "VAT"
      }

      "and links to the VAT Summary service" in {
        element(Selectors.vatDetailsBreadcrumbLink).attr("href") shouldBe "vat-details-url"
      }

      "have the 'Return deadlines' title" in {
        elementText(Selectors.returnDeadlinesBreadCrumb) shouldBe "Return deadlines"
      }
    }

    "have the correct document title" in {
      document.title shouldBe "Return deadlines"
    }

    "have the correct page heading" in {
      elementText(Selectors.pageHeading) shouldBe "Return deadlines"
    }

    "have the correct message regarding submitting returns through software" in {
      elementText(Selectors.submitThroughSoftware) shouldBe "You must use accounting software to submit a return by:"
    }

    "have the correct obligation due date" in {
      elementText(Selectors.firstDeadlineDueDate) should include ("2 February 2018")
    }

    "have the correct obligation start and end date text" in {
      elementText(Selectors.firstDeadlinePeriod) shouldBe "for the period 1 January to 1 January 2018"
    }

    "have the correct hint box title" in {
      elementText(Selectors.howToDoThis) shouldBe "How to submit a return"
    }

    "have the correct message regarding downloading software in the hint box" in {
      elementText(Selectors.downloadSoftware) shouldBe "Choose accounting software that supports this service if you haven't already."
    }

    "have the correct message regarding VAT records in the hint box" in {
      elementText(Selectors.vatRecords) shouldBe "Keep your VAT records in your accounting software."
    }

    "have the correct message regarding sending HMRC VAT returns in the hint box" in {
      elementText(Selectors.sendReturns) shouldBe "Submit any VAT Returns before your deadlines."
    }
  }

  "Rendering the Return deadlines page with multiple deadlines" should {

    val multipleDeadlines = Seq(
      ReturnDeadlineViewModel(
        LocalDate.parse("2018-02-02"),
        LocalDate.parse("2018-01-01"),
        LocalDate.parse("2018-01-01")),
      ReturnDeadlineViewModel(
        LocalDate.parse("2018-10-12"),
        LocalDate.parse("2018-09-07"),
        LocalDate.parse("2018-09-07"),
        overdue = true)
    )

    lazy val view = views.html.returns.returnDeadlines(multipleDeadlines, userVrn)
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
  }

  "Rendering the Return deadlines page with no deadlines" should {

    val noDeadlines = Seq()
    lazy val view = views.html.returns.returnDeadlines(noDeadlines, userVrn)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct text for no deadlines" in {
      elementText(Selectors.noReturnsNextDeadline) shouldBe
        "Your next deadline will show here on the first day of your next accounting period."
    }

    "have the correct software guidance" in {
      elementText(Selectors.noReturnsDue) shouldBe "You don't have any returns due right now."
    }
  }
}
