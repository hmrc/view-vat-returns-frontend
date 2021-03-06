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

package views.templates.formatters.breadcrumbs

import play.twirl.api.Html
import views.html.templates.formatters.breadcrumbs.NavigationBreadcrumb
import views.templates.TemplateBaseSpec

class NavigationBreadcrumbTemplateSpec extends TemplateBaseSpec {

  val injectedTemplate: NavigationBreadcrumb = inject[NavigationBreadcrumb]

  "Calling navigationBreadcrumb" should {

    val navigationMap: Map[String, String] = Map(
      "/link-url1" -> "link text1",
      "/link-url2" -> "link text2"
    )

    val currentPage = "current page"

    val expectedMarkup = Html(
      s"""
         |<div class="govuk-breadcrumbs govuk-!-margin-bottom-9">
         |    <ol class="govuk-breadcrumbs__list">
         |        <li class="govuk-breadcrumbs__list-item"><a class="govuk-breadcrumbs__link" href="/link-url1">link text1</a></li>
         |        <li class="govuk-breadcrumbs__list-item"><a class="govuk-breadcrumbs__link" href="/link-url2">link text2</a></li>
         |        <li class="govuk-breadcrumbs__list-item" aria-current="page">$currentPage</li>
         |    </ol>
         |</div>
      """.stripMargin
    )

    val markup = injectedTemplate(navigationMap, currentPage)

    "return the correct markup" in {
      formatHtml(markup) shouldBe formatHtml(expectedMarkup)
    }
  }
}
