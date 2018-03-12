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

import play.twirl.api.Html
import views.templates.TemplateBaseSpec

class SubmittedReturnsTabsTemplateSpec extends TemplateBaseSpec {

  "The submitted returns tabs template" should {

    "render a series of tabs, with the selected tab rendered as active" in {
      val tab1Year = "2022"
      val tab2Year = "2023"
      val tab3Year = "2024"
      val hiddenText1 = "View returns from 2022"
      val hiddenText2 = "Currently viewing returns from 2023"
      val hiddenText3 = "View returns from 2024"
      def tabUrl(year: String): String = s"/vat-through-software/vat-returns/submitted/$year"

      val tabs = Seq(2022, 2023, 2024)
      val selectedYear = 2023

      val expectedMarkup = Html(
        s"""
          |<li class="tabs-nav__tab font-medium">
          |  <a href="${tabUrl(tab1Year)}">
          |    $tab1Year
          |    <span class="visuallyhidden">$hiddenText1</span>
          |  </a>
          |</li>
          |<li class="tabs-nav__tab tabs-nav__tab--active font-medium">
          |  $tab2Year
          |  <span class="visuallyhidden">$hiddenText2</span>
          |</li>
          |<li class="tabs-nav__tab font-medium">
          |  <a href="${tabUrl(tab3Year)}">
          |    $tab3Year
          |    <span class="visuallyhidden">$hiddenText3</span>
          |  </a>
          |</li>
        """.stripMargin
      )
      val result = views.html.templates.returns.submittedReturnsTabs(tabs, selectedYear)

      formatHtml(result) shouldBe formatHtml(expectedMarkup)
    }
  }
}