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

package views.templates.returns

import java.time.LocalDate

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.html.templates.returns.PaymentStatus
import views.templates.TemplateBaseSpec

class PaymentStatusTemplateSpec extends TemplateBaseSpec {

  val injectedTemplate: PaymentStatus = inject[PaymentStatus]

  val whatYouOweCalc = "We have worked out what you owe based on any payments you might have made on your account. "

  val whatYouOweCalcAgent = "We have worked out what your client owes based on any payments made on their account. "

  "A principal user" should {
    "render the payment status information" when {

      "the User is NOT Hybrid and" when {

        "the user owes money on their VAT return" should {

          val expectedText = whatYouOweCalc +
            "You need to pay this bill by 11 November 2011. " +
            "It can take up to 7 days to show that you have made a payment. " +
            "Return total: £1,000 " +
            "You owe HMRC: £1,000"

          val template = injectedTemplate(
            1000,
            dueDate = LocalDate.parse("2011-11-11"),
            moneyOwed = true,
            oweHmrc = Some(true),
            isHybridUser = false,
            outstandingAmount = Some(1000)
          )(user = user, messages = messages)
          val document: Document = Jsoup.parse(template.body)

          "render the expected text" in {
            document.body().text() shouldBe expectedText
          }
        }

        "the outstanding amount on the return is zero" should {

          val expectedText = whatYouOweCalc + "Return total: £0 You owe HMRC: £0"

          val template = injectedTemplate(
            0,
            dueDate = LocalDate.parse("2011-11-11"),
            moneyOwed = false,
            oweHmrc = Some(false),
            isHybridUser = false,
            outstandingAmount = Some(0)
          )(user = user, messages = messages)
          val document: Document = Jsoup.parse(template.body)

          "render the expected text" in {
            document.body().text() shouldBe expectedText
          }
        }

        "the outstanding amount on the return is less than the return amount" should {

          val expectedText = whatYouOweCalc +
            "You need to pay this bill by 11 November 2011. " +
            "It can take up to 7 days to show that you have made a payment. " +
            "Return total: £1,000 " +
            "You owe HMRC: £500"

          val template = injectedTemplate(
            1000,
            dueDate = LocalDate.parse("2011-11-11"),
            moneyOwed = true,
            oweHmrc = Some(true),
            isHybridUser = false,
            outstandingAmount = Some(500)
          )(user = user, messages = messages)
          val document: Document = Jsoup.parse(template.body)

          "render the expected text" in {
            document.body().text() shouldBe expectedText
          }
        }

        "the user is waiting for HMRC to pay them back for their VAT return" should {

          val expectedText = whatYouOweCalc + "It can take up to 30 days for you to receive a repayment. " +
            "Return total: £1,000 HMRC owes you: £1,000"

          val template = injectedTemplate(
            1000,
            dueDate = LocalDate.parse("2011-11-11"),
            moneyOwed = true,
            oweHmrc = Some(false),
            isHybridUser = false,
            outstandingAmount = Some(-1000)
          )(user = user, messages = messages)
          val document: Document = Jsoup.parse(template.body)

          "render the expected text" in {
            document.body().text() shouldBe expectedText
          }
        }
      }

      "the User is Hybrid" should {

        val expectedText = "Return total: £1,000"

        val template = injectedTemplate(
          1000,
          dueDate = LocalDate.parse("2011-11-11"),
          oweHmrc = Some(false),
          moneyOwed = false,
          isHybridUser = true,
          outstandingAmount = Some(1000)
        )(user = user, messages = messages)
        val document: Document = Jsoup.parse(template.body)

        "render the expected text" in {
          document.body().text() shouldBe expectedText
        }

        "not render the paragraph about how what you owe has been calculated" in {
          document.body().text() shouldNot contain(whatYouOweCalc)
        }

        "not render what HMRC owes the user" in {
          document.body().text() shouldNot contain("HMRC owes you: £1,000")
        }
      }
    }
  }
  "An agent" should {
    "render the payment status information" when {

      "the User is NOT Hybrid and" when {

        "the user owes money on their VAT return" should {

          val expectedText = whatYouOweCalcAgent +
            "Your client needs to pay this bill by 11 November 2011. " +
            "It can take up to 7 days to show that they have made a payment. " +
            "Return total: £1,000 " +
            "Your client owes HMRC: £1,000"

          val template = injectedTemplate(
            1000,
            dueDate = LocalDate.parse("2011-11-11"),
            moneyOwed = true,
            oweHmrc = Some(true),
            isHybridUser = false,
            outstandingAmount = Some(1000)
          )(user = agentUser, messages = messages)
          val document: Document = Jsoup.parse(template.body)

          "render the expected text" in {
            document.body().text() shouldBe expectedText
          }
        }

        "the outstanding amount on the return is zero" should {

          val expectedText = whatYouOweCalcAgent + "Return total: £0 Your client owes HMRC: £0"

          val template = injectedTemplate(
            0,
            dueDate = LocalDate.parse("2011-11-11"),
            moneyOwed = false,
            oweHmrc = Some(false),
            isHybridUser = false,
            outstandingAmount = Some(0)
          )(user = agentUser, messages = messages)
          val document: Document = Jsoup.parse(template.body)

          "render the expected text" in {
            document.body().text() shouldBe expectedText
          }
        }

        "the outstanding amount on the return is less than the return amount" should {

          val expectedText = whatYouOweCalcAgent +
            "Your client needs to pay this bill by 11 November 2011. " +
            "It can take up to 7 days to show that they have made a payment. " +
            "Return total: £1,000 " +
            "Your client owes HMRC: £500"

          val template = injectedTemplate(
            1000,
            dueDate = LocalDate.parse("2011-11-11"),
            moneyOwed = true,
            oweHmrc = Some(true),
            isHybridUser = false,
            outstandingAmount = Some(500)
          )(user = agentUser, messages = messages)
          val document: Document = Jsoup.parse(template.body)

          "render the expected text" in {
            document.body().text() shouldBe expectedText
          }
        }
      }

      "the User is Hybrid" should {

        val expectedText = "Return total: £1,000"

        val template = injectedTemplate(
          1000,
          dueDate = LocalDate.parse("2011-11-11"),
          oweHmrc = Some(false),
          moneyOwed = false,
          isHybridUser = true,
          outstandingAmount = Some(1000)
        )(user = agentUser, messages = messages)
        val document: Document = Jsoup.parse(template.body)

        "render the expected text" in {
          document.body().text() shouldBe expectedText
        }

        "not render the paragraph about how what you owe has been calculated" in {
          document.body().text() shouldNot contain(whatYouOweCalcAgent)
        }

        "not render what HMRC owes the client" in {
          document.body().text() shouldNot contain("HMRC owes you: £1,000")
        }
      }
    }
  }
}
