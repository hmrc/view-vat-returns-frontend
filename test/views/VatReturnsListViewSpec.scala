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

import models.{VatReturn, VatReturns}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class VatReturnsListViewSpec extends ViewBaseSpec {

  object Selectors {
    val pageHeading = "#content h1"
    val submitThroughSoftware = "#content > article > div > div > p:nth-child(2)"
    val tableCaption = "#content caption"
    val periodEndingColumnHeading = "#vatReturnsList > thead > tr > th:nth-child(1)"
    val statusColumnHeading = "#vatReturnsList > thead > tr > th:nth-child(2)"
    val returnDetailsColumnHeading = "#vatReturnsList > thead > tr > th:nth-child(3)"
    val periodEndingOutstanding = "#vatReturnsList > tbody:nth-child(3) > tr:nth-child(1) > td:nth-child(1)"
    val periodEndingFulfilled = "#vatReturnsList > tbody:nth-child(3) > tr:nth-child(2) > td:nth-child(1)"
    val statusOutstanding = "#vatReturnsList > tbody:nth-child(3) > tr:nth-child(1) > td:nth-child(2)"
    val statusFulfilled = "#vatReturnsList > tbody:nth-child(3) > tr:nth-child(2) > td:nth-child(2)"
    val detailsOutstanding = "#vatReturnsList > tbody:nth-child(3) > tr:nth-child(1) > td:nth-child(3)"
    val detailsFulfilled = "#vatReturnsList > tbody:nth-child(3) > tr:nth-child(2) > td:nth-child(3) > a"
    val noReturns = "#content h2"
    val earlierReturns = "#content > article > div > div > p:nth-child(4)"
  }

  "Rendering the VAT Returns page" should {

    lazy val view = views.html.returns.vatReturnsList(VatReturns(Seq()))
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

    "have the correct message regarding viewing earlier returns" in {
      elementText(Selectors.earlierReturns) shouldBe "You can also view earlier returns you submitted before using accounting software."
    }
  }

  "Rendering the VAT Returns page with one outstanding and one fulfilled VAT Return" should {

    lazy val exampleReturns: VatReturns = VatReturns(
      Seq(
        VatReturn(
          LocalDate.parse("2017-01-01"),
          LocalDate.parse("2017-12-31"),
          LocalDate.parse("2018-01-31"),
          "O",
          None,
          "#001"
        ),
        VatReturn(
          LocalDate.parse("2017-01-01"),
          LocalDate.parse("2017-09-30"),
          LocalDate.parse("2018-10-31"),
          "F",
          None,
          "#001"
        )
      )
    )

    lazy val view = views.html.returns.vatReturnsList(exampleReturns)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct table caption" in {
      elementText(Selectors.tableCaption) shouldBe "VAT returns found since opting in for Making Tax Digital For Business"
    }

    "have the correct heading for the period ending column" in {
      elementText(Selectors.periodEndingColumnHeading) shouldBe "Period ending"
    }

    "have the correct heading for the status column" in {
      elementText(Selectors.statusColumnHeading) shouldBe "Status"
    }

    "have the correct heading for the return details column" in {
      elementText(Selectors.returnDetailsColumnHeading) shouldBe "Return details"
    }

    "have the correct period ending for the outstanding return" in {
      elementText(Selectors.periodEndingOutstanding) shouldBe "31 December 2017"
    }

    "have the correct period ending for the fulfilled return" in {
      elementText(Selectors.periodEndingFulfilled) shouldBe "30 September 2017"
    }

    "have the correct status for the outstanding return" in {
      elementText(Selectors.statusOutstanding) shouldBe "Due 31 January 2018"
    }

    "have the correct status for the fulfilled return" in {
      elementText(Selectors.statusFulfilled) shouldBe "Received"
    }

    "have the correct return details for the outstanding return" in {
      elementText(Selectors.detailsOutstanding) shouldBe "Not yet submitted"
    }

    "have the correct return details for the fulfilled return" in {
      elementText(Selectors.detailsFulfilled) shouldBe "View 30 September 2017 Return"
    }
  }

  "Rendering the VAT Returns page with no available VAT Returns" should {

    lazy val view = views.html.returns.vatReturnsList(VatReturns(Seq()))
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct message that there are no returns" in {
      elementText(Selectors.noReturns) shouldBe "You have no returns."
    }
  }
}
