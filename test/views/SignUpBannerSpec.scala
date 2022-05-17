/*
 * Copyright 2022 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import views.html.SignUpBanner

class SignUpBannerSpec extends ViewBaseSpec {

  val injectedView: SignUpBanner = inject[SignUpBanner]

  object Selectors {
    val banner = ".govuk-notification-banner"
    val title = "#govuk-notification-banner-title"
    val heading = ".govuk-heading-m"
    val text = ".govuk-body"
    val link = "a"
  }

  "SignUpBanner" when {

    "the user has a mandation status of 'Non MTDfB'" should {

      lazy val view = injectedView("Non MTDfB")
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "display a banner" that {

        "has the correct title" in {
          elementText(Selectors.title) shouldBe "Important"
        }

        "has the correct subheading" in {
          elementText(Selectors.heading) shouldBe "The way to submit VAT returns changed on 1 April due to Making Tax Digital"
        }

        "has the correct text" in {
          elementText(Selectors.text) shouldBe "You cannot use this service to submit returns for accounting periods " +
            "starting after 1 April 2022. Instead, digital records must be kept and returns must be submitted using HMRC compatible " +
            "software. Find out when to sign up and start using Making Tax Digital for VAT (opens in a new tab)."
        }

        "has a link" that {

          "has the correct text" in {
            elementText(Selectors.link) shouldBe "Find out when to sign up and start using Making Tax Digital for VAT (opens in a new tab)"
          }

          "has the correct href" in {
            element(Selectors.link).attr("href") shouldBe mockConfig.govUkSignUpGuideUrl
          }
        }
      }
    }

    "the user has a mandation status of 'Non Digital'" should {

      lazy val view = injectedView("Non Digital")
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "not display a banner" in {
        elementExtinct(Selectors.banner)
      }
    }

    "the user has a mandation status of 'MTDfB Exempt'" should {

      lazy val view = injectedView("MTDfB Exempt")
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "not display a banner" in {
        elementExtinct(Selectors.banner)
      }
    }

    "the user has a mandation status of 'MTDfB'" should {

      lazy val view = injectedView("MTDfB")
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "not display a banner" in {
        elementExtinct(Selectors.banner)
      }
    }

    "the mandation status isn't in session" should {

      lazy val view = injectedView("")
      lazy implicit val document: Document = Jsoup.parse(view.body)

      "not display a banner" in {
        elementExtinct(Selectors.banner)
      }
    }
  }
}
