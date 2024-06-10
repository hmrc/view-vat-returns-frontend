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

package views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import views.html.MainTemplate

class MainTemplateSpec extends ViewBaseSpec {

  val injectedView: MainTemplate = inject[MainTemplate]
  val userBannerHeading: String = "Help make GOV.UK better"

  object Selectors {
    val serviceName: String = ".govuk-header__service-name"
    val userResearchBanner: String = ".hmrc-user-research-banner"
    val userResearchBannerHeading = ".hmrc-user-research-banner__title"
  }

  "MainTemplate" when {

    "the showUserResearchBanner feature switch is turned off" when {

      "the user is an Agent" should {
        lazy val view = injectedView(pageTitle = "", user = Some(agentUser))(Html("Test"))(request, messages, mockConfig)
        lazy implicit val document: Document = Jsoup.parse(view.body)

        "have the correct service name" in {
          elementText(Selectors.serviceName) shouldBe "Your client’s VAT details"
        }

        "have the correct service URL" in {
          element(Selectors.serviceName).attr("href") shouldBe mockConfig.agentClientHubUrl
        }

        "not display the user research banner" in {
          mockConfig.features.showUserResearchBannerEnabled(false)
          elementExtinct(Selectors.userResearchBanner)
        }
      }

      "the user is not an Agent" should {
        lazy val view = injectedView(pageTitle = "", user = Some(user))(Html("Test"))(request, messages, mockConfig)
        lazy implicit val document: Document = Jsoup.parse(view.body)

        "have the correct service name" in {
          elementText(Selectors.serviceName) shouldBe "Manage your VAT account"
        }

        "have the correct service URL" in {
          element(Selectors.serviceName).attr("href") shouldBe mockConfig.vatDetailsUrl
        }

        "not display the user research banner" in {
          mockConfig.features.showUserResearchBannerEnabled(false)
          elementExtinct(Selectors.userResearchBanner)
        }
      }

      "the user type cannot be determined" should {
        lazy val view = injectedView(pageTitle = "", user = None)(Html("Test"))(request, messages, mockConfig)
        lazy implicit val document: Document = Jsoup.parse(view.body)

        "have the correct service name" in {
          elementText(Selectors.serviceName) shouldBe "VAT"
        }

        "have the correct service URL" in {
          element(Selectors.serviceName).attr("href") shouldBe ""
        }

        "not display the user research banner" in {
          mockConfig.features.showUserResearchBannerEnabled(false)
          elementExtinct(Selectors.userResearchBanner)
        }
      }
    }

    "the showUserResearchBanner feature switch is turned on" when {

      "the user is an Agent" should {
        lazy val view = injectedView(pageTitle = "", user = Some(agentUser))(Html("Test"))(request, messages, mockConfig)
        lazy implicit val document: Document = Jsoup.parse(view.body)

        "display the correct user research banner heading" in {
          mockConfig.features.showUserResearchBannerEnabled(true)
          elementText(Selectors.userResearchBannerHeading) shouldBe userBannerHeading
        }
      }

      "the user is not an Agent" should {
        lazy val view = injectedView(pageTitle = "", user = Some(user))(Html("Test"))(request, messages, mockConfig)
        lazy implicit val document: Document = Jsoup.parse(view.body)

        "display the correct user research banner content" in {
          mockConfig.features.showUserResearchBannerEnabled(true)
          elementText(Selectors.userResearchBannerHeading) shouldBe userBannerHeading
        }
      }

      "the user type cannot be determined" should {
        lazy val view = injectedView(pageTitle = "", user = None)(Html("Test"))(request, messages, mockConfig)
        lazy implicit val document: Document = Jsoup.parse(view.body)

        "display the correct user research banner content" in {
          mockConfig.features.showUserResearchBannerEnabled(true)
          elementText(Selectors.userResearchBannerHeading) shouldBe userBannerHeading
        }
      }
    }
  }
}
