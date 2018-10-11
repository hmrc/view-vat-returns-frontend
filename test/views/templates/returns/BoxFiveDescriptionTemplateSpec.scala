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

package views.templates.returns

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.templates.TemplateBaseSpec

class BoxFiveDescriptionTemplateSpec extends TemplateBaseSpec {

  "Rendering the Box 5 row of the VAT return table" when {

    "the user owes money on their VAT return" should {

      val template = views.html.templates.returns.boxFiveDescription(moneyOwed = true, isRepayment = false, isHybridUser = false)
      val document: Document = Jsoup.parse(template.body)

      "render the expected text" in {
        document.body().text() shouldBe "Total VAT you owe"
      }
    }

    "the user has paid off their VAT return" should {

      val template = views.html.templates.returns.boxFiveDescription(moneyOwed = false, isRepayment = false, isHybridUser = false)
      val document: Document = Jsoup.parse(template.body)

      "render the expected text" in {
        document.body().text() shouldBe "Total VAT you owed"
      }
    }

    "the user is waiting for HMRC to pay them back for their VAT return" should {

      val template = views.html.templates.returns.boxFiveDescription(moneyOwed = true, isRepayment = true, isHybridUser = false)
      val document: Document = Jsoup.parse(template.body)

      "render the expected text" in {
        document.body().text() shouldBe "Total VAT HMRC owes you"
      }
    }

    "the user has been paid by HMRC for their VAT return" should {

      val template = views.html.templates.returns.boxFiveDescription(moneyOwed = false, isRepayment = true, isHybridUser = false)
      val document: Document = Jsoup.parse(template.body)

      "render the expected text" in {
        document.body().text() shouldBe "Total VAT HMRC owed you"
      }
    }

    "the user is hybrid and" when {

      "a repayment from HMRC is due" should {

        val template = views.html.templates.returns.boxFiveDescription(moneyOwed = false, isRepayment = true, isHybridUser = true)
        val document: Document = Jsoup.parse(template.body)

        "render the expected text" in {
          document.body().text() shouldBe "Total VAT HMRC owes you"
        }
      }

      "a repayment is NOT due from HMRC" should {

        val template = views.html.templates.returns.boxFiveDescription(moneyOwed = false, isRepayment = false, isHybridUser = true)
        val document: Document = Jsoup.parse(template.body)

        "render the expected text" in {
          document.body().text() shouldBe "Total VAT you owe"
        }
      }
    }
  }
}
