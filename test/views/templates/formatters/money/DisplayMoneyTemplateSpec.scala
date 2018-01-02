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

package views.templates.formatters.money

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.templates.TemplateBaseSpec

class DisplayMoneyTemplateSpec extends TemplateBaseSpec {

  "Calling displayMoney" when {

    "the amount is a whole number" should {

      lazy val money = BigDecimal("9999999")
      lazy val template = views.html.templates.formatters.money.displayMoney(money)
      lazy val document: Document = Jsoup.parse(template.body)

      "render the amount with the correct formatting" in {
        document.body().text() shouldEqual "£9,999,999"
      }
    }

    "the amount is not a whole number" should {

      lazy val money = BigDecimal("9999.99")
      lazy val template = views.html.templates.formatters.money.displayMoney(money)
      lazy val document: Document = Jsoup.parse(template.body)

      "render the amount with the correct formatting" in {
        document.body().text() shouldEqual "£9,999.99"
      }
    }
  }
}
