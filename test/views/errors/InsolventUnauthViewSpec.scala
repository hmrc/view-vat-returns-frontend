/*
 * Copyright 2021 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.ViewBaseSpec
import views.html.errors.InsolventUnauthView

class InsolventUnauthViewSpec extends ViewBaseSpec {

  val injectedView: InsolventUnauthView = inject[InsolventUnauthView]

  "Rendering the unauthorised page for insolvent users" should {

    object Selectors {
      val pageHeading = "#content h1"
      val message = "#insolvent-without-access-body"
      val button = ".govuk-button"
      val signOut = "#sign-out-link"
      val signOutLink = "#sign-out-link"
    }

    lazy val view = injectedView(user)
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have the correct document title" in {
      document.title shouldBe "Sorry, you cannot access this service - Manage your VAT account - GOV.UK"
    }

    "have a the correct page heading" in {
      elementText(Selectors.pageHeading) shouldBe "Sorry, you cannot access this service"
    }

    "have the correct message body" in {
      elementText(Selectors.message) shouldBe "Your business has been declared insolvent."
    }

    "have the correct sign out text" in {
      elementText(Selectors.signOut) shouldBe "Sign out"
    }

    "have the correct sign out link" in {
      element(Selectors.signOutLink).attr("href") shouldBe "/vat-through-software/vat-returns/sign-out?authorised=false"
    }

    "have the correct button text" in {
      elementText(Selectors.button) shouldBe "Go to your business tax account"
    }

    "have the correct button link" in {
      element(Selectors.button).attr("href") shouldBe "bta-url"
    }

  }
}
