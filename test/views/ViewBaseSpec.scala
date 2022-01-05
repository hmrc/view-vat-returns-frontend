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

import common.SessionKeys
import mocks.MockAppConfig
import models.User
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{FakeRequest, Injecting}

import scala.collection.JavaConverters._


class ViewBaseSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite with Injecting {

  implicit lazy val mockConfig: MockAppConfig = new MockAppConfig(app.configuration)
  implicit lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  lazy val messagesApi: MessagesApi = inject[MessagesApi]
  implicit lazy val messages: Messages = MessagesImpl(Lang("en-GB"), messagesApi)
  lazy val vrn = "999999999"
  lazy val arn = "XAIT00000000000"
  lazy val fakeRequestWithClientsVRN: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest().withSession(SessionKeys.mtdVatvcClientVrn -> vrn)
  implicit val user: User = User(vrn)
  lazy val agentUser: User = User(vrn, active = true, hasNonMtdVat = true, Some(arn))

  def element(cssSelector: String)(implicit document: Document): Element = {
    val elements = document.select(cssSelector)

    if (elements.size == 0) {
      fail(s"No element exists with the selector '$cssSelector'")
    }

    document.select(cssSelector).first()
  }

  def elementAsOpt(cssSelector: String)(implicit document: Document): Option[Element] = {
    val elements = document.select(cssSelector)

    if(elements.isEmpty) {
      None
    } else {
      Some(document.select(cssSelector).first())
    }
  }

  def elementText(selector: String)(implicit document: Document): String = {
    element(selector).text()
  }

  def elementAttributes(cssSelector: String)(implicit document: Document): Map[String, String] = {
    val attributes = element(cssSelector).attributes.asList().asScala.toList
    attributes.map(attribute => (attribute.getKey, attribute.getValue)).toMap
  }

  def elementExtinct(cssSelector: String)(implicit document: Document): Assertion = {
    val elements = document.select(cssSelector)

    if (elements.size == 0) {
      succeed
    } else {
      fail(s"Element with selector '$cssSelector' was found!")
    }
  }

  def formatHtml(markup: String): String = Jsoup.parseBodyFragment(s"\n$markup\n").toString.trim
}
