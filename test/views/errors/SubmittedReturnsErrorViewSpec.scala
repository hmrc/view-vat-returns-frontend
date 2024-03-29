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

package views.errors

import models.User
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.ViewBaseSpec
import views.html.errors.SubmittedReturnsErrorView

class SubmittedReturnsErrorViewSpec extends ViewBaseSpec {

  val injectedView: SubmittedReturnsErrorView = inject[SubmittedReturnsErrorView]

  object Selectors {
    val heading = "h1"
    val ledeText = "#content > p.govuk-body"
    val previousReturn = "p:nth-of-type(2)"
    val previousReturnsLink = s"$previousReturn > a"
  }

  private val userWithNonMtdVat = User("111111111", hasNonMtdVat = true)
  override val user = User("111111111")

  "Rendering the submitted returns error view" should {

    lazy val view = injectedView(user)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "There is a problem with the service - Manage your VAT account - GOV.UK"
    }

    "have the correct document heading" in {
      elementText(Selectors.heading) shouldBe "Sorry, there is a problem with the service"
    }

    "have the correct try again message" in {
      elementText(Selectors.ledeText) shouldBe "Try again later."
    }
  }

  "Rendering the submitted returns error view with a non mtd vat enrolment" should {

    lazy val view = injectedView(userWithNonMtdVat)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "There is a problem with the service - Manage your VAT account - GOV.UK"
    }

    "have the correct document heading" in {
      elementText(Selectors.heading) shouldBe "Sorry, there is a problem with the service"
    }

    "have the correct try again message" in {
      elementText(Selectors.ledeText) shouldBe "Try again later."
    }

    "have the correct previous returns message" in {
      elementText(Selectors.previousReturn) shouldBe "If you have submitted returns without using the software for this new service," +
        " you can view your previous returns (opens in a new tab)."
    }

    "have the correct link" in {
      element(Selectors.previousReturnsLink).attr("href") shouldBe "/portal-url/111111111"
    }
  }
}
