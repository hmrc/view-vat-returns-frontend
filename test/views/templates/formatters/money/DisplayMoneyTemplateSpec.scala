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

package views.templates.formatters.money

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.html.templates.formatters.money.DisplayMoney
import views.templates.TemplateBaseSpec

class DisplayMoneyTemplateSpec extends TemplateBaseSpec {

  val injectedTemplate: DisplayMoney = inject[DisplayMoney]

  "Calling displayMoney" when {

    "the amount is a whole number" should {

      lazy val money = BigDecimal("9999999")
      lazy val template = injectedTemplate(money)
      lazy val document: Document = Jsoup.parse(template.body)

      "render the amount with the correct formatting" in {
        document.body().text() shouldEqual "£9,999,999"
      }
    }

    "the amount is not a whole number" should {

      lazy val money = BigDecimal("9999.99")
      lazy val template = injectedTemplate(money)
      lazy val document: Document = Jsoup.parse(template.body)

      "render the amount with the correct formatting" in {
        document.body().text() shouldEqual "£9,999.99"
      }
    }

    "the amount is negative" should {

      lazy val money = BigDecimal("-1")
      lazy val template = injectedTemplate(money)
      lazy val document: Document = Jsoup.parse(template.body)

      "render the amount with a negative prefix" in {
        document.body().text() shouldEqual "−£1"
      }
    }

    "the amount is zero" should {

      lazy val money = BigDecimal("0")
      lazy val template = injectedTemplate(money)
      lazy val document: Document = Jsoup.parse(template.body)

      "render the amount without a negative prefix" in {
        document.body().text() shouldEqual "£0"
      }
    }

    "the amount is positive" should {

      lazy val money = BigDecimal("1")
      lazy val template = injectedTemplate(money)
      lazy val document: Document = Jsoup.parse(template.body)

      "render the amount without a negative prefix" in {
        document.body().text() shouldEqual "£1"
      }
    }
  }
}
