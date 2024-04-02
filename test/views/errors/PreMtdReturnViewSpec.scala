/*
 * Copyright 2024 HM Revenue & Customs
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

package views.errors

import models.User
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.ViewBaseSpec
import views.html.errors.PreMtdReturnView

class PreMtdReturnViewSpec extends ViewBaseSpec {

  val injectedView: PreMtdReturnView = inject[PreMtdReturnView]

  object Selectors {
    val heading = "h1"
    val backlink = ".govuk-back-link"
    val text = "#pre-mtd-p1"
    val link = "#pre-mtd-link > a"
  }

  private val userWithNonMtdVat = User("111111111", hasNonMtdVat = true)
  override val user = User("111111111")

  "Rendering the pre mtd return error view without the HMCE-VATDEC-ORG enrolment" should {

    lazy val view = injectedView(user)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "This return is not available - Manage your VAT account - GOV.UK"
    }

    "have a back link" that {

      "has the correct text" in {
        elementText(Selectors.backlink) shouldBe "Back"
      }

      "has the correct href" in {
        element(Selectors.backlink).attr("href") shouldBe "/vat-through-software/vat-returns/submitted"
      }
    }

    "have the correct document heading" in {
      elementText(Selectors.heading) shouldBe "This return is not available"
    }

    "have the correct message" in {
      elementText(Selectors.text) shouldBe "This is because you might have submitted it before you first joined Making Tax Digital for VAT."
    }

    "not have a link to view the return in portal" in {
      elementAsOpt(Selectors.link) shouldBe None
    }
  }

  "Rendering the pre mtd return error view with the HMCE-VATDEC-ORG enrolment" should {

    lazy val view = injectedView(userWithNonMtdVat)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "This return is not available - Manage your VAT account - GOV.UK"
    }

    "have the correct document heading" in {
      elementText(Selectors.heading) shouldBe "This return is not available"
    }

    "have the correct message" in {
      elementText(Selectors.text) shouldBe "This is because you might have submitted it before you first joined Making Tax Digital for VAT."
    }

    "have the correct link" in {
      element(Selectors.link).attr("href") shouldBe "/portal-url/111111111"
    }
  }
}
