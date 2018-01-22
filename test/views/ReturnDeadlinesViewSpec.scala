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

import models.viewModels.ReturnDeadline
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class ReturnDeadlinesViewSpec extends ViewBaseSpec {

  object Selectors {
    val pageHeading = "#content h1"
    val submitThroughSoftware = "#content > article > div > div > p"
    val dueDate = "#content li"
    val endDate = "#content li > span"
    val howToDoThis = "#content article > div > div > details > summary > span"
    val downloadSoftware = "#content article > div > div > details > div > p:nth-child(1)"
    val vatRecords = "#content article > div > div > details > div > p:nth-child(2)"
    val sendReturns= "#content article > div > div > details > div > p:nth-child(3)"
  }

  "Rendering the Return deadlines page" should {

    val exampleDeadline: ReturnDeadline = ReturnDeadline(LocalDate.parse("2018-02-02"), LocalDate.parse("2018-01-01"))

    lazy val view = views.html.returns.returnDeadlines(exampleDeadline)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "Return deadlines"
    }

    "have the correct page heading" in {
      elementText(Selectors.pageHeading) shouldBe "Return deadlines"
    }

    "have the correct message regarding submitting returns through software" in {
      elementText(Selectors.submitThroughSoftware) shouldBe "You submit returns through your accounting software."
    }

    "have the correct obligation due date" in {
      elementText(Selectors.dueDate) should include ("2 February 2018")
    }

    "have the correct obligation end date" in {
      elementText(Selectors.endDate) shouldBe "for the period ending 1 January 2018"
    }

    "have the correct hint box title" in {
      elementText(Selectors.howToDoThis) shouldBe "How to do this"
    }

    "have the correct message regarding downloading software in the hint box" in {
      elementText(Selectors.downloadSoftware) shouldBe "Download accounting software from a listed supplier, if you haven't already."
    }

    "have the correct message regarding VAT records in the hint box" in {
      elementText(Selectors.vatRecords) shouldBe "Keep your VAT records in that accounting software."
    }

    "have the correct message regarding sending HMRC VAT returns in the hint box" in {
      elementText(Selectors.sendReturns) shouldBe "Send HMRC your returns using that same accounting software."
    }
  }
}
