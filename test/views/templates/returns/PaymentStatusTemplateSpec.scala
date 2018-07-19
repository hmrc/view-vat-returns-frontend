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

import java.time.LocalDate

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.templates.TemplateBaseSpec

class PaymentStatusTemplateSpec extends TemplateBaseSpec {

  "Rendering the payment status information" when {

    "the user owes money on their VAT return" should {

      val expectedText = "You need to pay this bill by 11 November 2011. " +
           "It can take up to 7 days to show that you have made a payment. Return total: £1,000"

      val template = views.html.templates.returns.paymentStatus(
        1000,
        LocalDate.parse("2011-11-11"),
        moneyOwed = true,
        isRepayment = false
      )
      val document: Document = Jsoup.parse(template.body)

      "render the expected text" in {
        document.body().text() shouldBe expectedText
      }
    }

    "the user has paid off their VAT return" should {

      val expectedText = "You paid: £1,000"

      val template = views.html.templates.returns.paymentStatus(
        1000,
        LocalDate.parse("2011-11-11"),
        moneyOwed = false,
        isRepayment = false
      )
      val document: Document = Jsoup.parse(template.body)

      "render the expected text" in {
        document.body().text() shouldBe expectedText
      }
    }

    "the user is waiting for HMRC to pay them back for their VAT return" should {

      val expectedText = "It can take up to 30 days for you to receive a repayment. HMRC will pay you: £1,000"

      val template = views.html.templates.returns.paymentStatus(
        1000,
        LocalDate.parse("2011-11-11"),
        moneyOwed = true,
        isRepayment = true
      )
      val document: Document = Jsoup.parse(template.body)

      "render the expected text" in {
        document.body().text() shouldBe expectedText
      }
    }

    "the user has been paid by HMRC for their VAT return" should {

      val expectedText = "HMRC paid you: £1,000"

      val template = views.html.templates.returns.paymentStatus(
        1000,
        LocalDate.parse("2011-11-11"),
        moneyOwed = false,
        isRepayment = true
      )
      val document: Document = Jsoup.parse(template.body)

      "render the expected text" in {
        document.body().text() shouldBe expectedText
      }
    }
  }
}
