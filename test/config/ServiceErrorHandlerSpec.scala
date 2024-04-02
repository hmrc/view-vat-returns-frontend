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

package config

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.ViewBaseSpec
import views.html.errors.StandardErrorView

class ServiceErrorHandlerSpec extends ViewBaseSpec {

  val service: ServiceErrorHandler = new ServiceErrorHandler(messagesApi, inject[StandardErrorView])

  object Selectors {
    val pageHeading = "h1"
    val message = "p.govuk-body"
  }

  "Calling .notFoundTemplate" should {

    lazy val view = service.notFoundTemplate
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "display the correct title" in {
      document.title shouldBe "Page not found - VAT - GOV.UK"
    }

    "display the correct page heading" in {
      elementText(Selectors.pageHeading) shouldBe "This page cannot be found"
    }

    "display the correct message" in {
      element(Selectors.message).text() shouldBe "Please check that you have entered the correct web address."
    }
  }

  "Calling .internalServerErrorTemplate" should {

    lazy val view = service.internalServerErrorTemplate
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "display the correct title" in {
      document.title shouldBe "There is a problem with the service - VAT - GOV.UK"
    }

    "display the correct page heading" in {
      elementText(Selectors.pageHeading) shouldBe "Sorry, there is a problem with the service"
    }

    "display the correct message" in {
      element(Selectors.message).text() shouldBe "Try again later."
    }
  }

  "Calling .standardErrorTemplate" should {

    lazy val view = service.standardErrorTemplate("errorTitle", "errorHeading", "errorMessage")
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "display the correct title" in {
      document.title shouldBe "errorTitle - VAT - GOV.UK"
    }

    "displays the correct page heading" in {
      elementText(Selectors.pageHeading) shouldBe "errorHeading"
    }

    "displays the correct message" in {
      element(Selectors.message).text() shouldBe "errorMessage"
    }
  }
}
