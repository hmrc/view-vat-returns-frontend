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

class NoUpcomingReturnDeadlinesViewSpec extends ViewBaseSpec {

  object Selectors {
    val pageHeading = "#content h1"

    val btaBreadcrumb = "div.breadcrumbs li:nth-of-type(1)"
    val btaBreadCrumbLink = "div.breadcrumbs li:nth-of-type(1) a"
    val vatDetailsBreadCrumb = "div.breadcrumbs li:nth-of-type(2)"
    val vatDetailsBreadcrumbLink = "div.breadcrumbs li:nth-of-type(2) a"
    val returnDeadlinesBreadCrumb = "div.breadcrumbs li:nth-of-type(3)"

    val noReturnsNextDeadline = "p.lede"
    val noReturnsDue = "article > p:nth-child(4)"
  }

  "Rendering the Return deadlines page with no fulfilled obligations" should {

    val noFulfilledObligation = None
    lazy val view = views.html.returns.noUpcomingReturnDeadlines(noFulfilledObligation)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "render the breadcrumbs which" should {

      "have the 'Business tax account' title" in {
        elementText(Selectors.btaBreadcrumb) shouldBe "Business tax account"
      }

      "and links to the BTA service" in {
        element(Selectors.btaBreadCrumbLink).attr("href") shouldBe "bta-url"
      }

      "have the 'VAT' title" in {
        elementText(Selectors.vatDetailsBreadCrumb) shouldBe "Your VAT details"
      }

      "and links to the VAT Summary service" in {
        element(Selectors.vatDetailsBreadcrumbLink).attr("href") shouldBe "vat-details-url"
      }

      "have the 'Return deadlines' title" in {
        elementText(Selectors.returnDeadlinesBreadCrumb) shouldBe "Return deadlines"
      }
    }

    "have the correct text for no deadlines" in {
      elementText(Selectors.noReturnsNextDeadline) shouldBe
        "Your next deadline will show here on the first day of your next accounting period."
    }

    "have the correct returns due guidance" in {
      elementText(Selectors.noReturnsDue) shouldBe "You don't have any returns due right now."
    }
  }

  "Rendering the Return deadlines page with a fulfilled obligation" should {

    val fulfilledObligation = Some(ReturnDeadlineViewModel(
      end = LocalDate.parse("2018-04-01"),
      start = LocalDate.parse("2018-01-01"),
      due = LocalDate.parse("2018-05-01")
    ))
    lazy val view = views.html.returns.noUpcomingReturnDeadlines(fulfilledObligation)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct text for no deadlines" in {
      elementText(Selectors.noReturnsNextDeadline) shouldBe
        "Your next deadline will show here on the first day of your next accounting period."
    }

    "have the correct received return guidance" in {
      elementText(Selectors.noReturnsDue) shouldBe
        "We received your return for the period 1 January to 1 April 2018. You don't have any returns due right now."
    }
  }
}