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

package connectors

import controllers.ControllerBaseSpec
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.twirl.api.Html
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.partials.{HeaderCarrierForPartialsConverter, HtmlPartial}
import uk.gov.hmrc.play.partials.HtmlPartial.{Failure, Success}
import views.html.templates.BtaNavigationLinks

import scala.concurrent.{ExecutionContext, Future}

class ServiceInfoConnectorSpec extends ControllerBaseSpec {

    val hcForPartials: HeaderCarrierForPartialsConverter = inject[HeaderCarrierForPartialsConverter]
    val btaNavigationLinks: BtaNavigationLinks = inject[BtaNavigationLinks]
    val validHtml: Html = Html("<nav>BTA LINK</nav>")
    val httpClient: HttpClient = mock[HttpClient]

    val connector: ServiceInfoConnector = new ServiceInfoConnector(httpClient, hcForPartials,
      btaNavigationLinks)(inject[MessagesApi], mockConfig)

    def mockConnectorCall(result: Future[HtmlPartial]): Any =
      (httpClient.GET[HtmlPartial](_: String, _: Seq[(String, String)], _: Seq[(String, String)])
        (_: HttpReads[HtmlPartial], _: HeaderCarrier, _: ExecutionContext))
        .stubs(*,*,*,*,*,*)
        .returns(result)

  "ServiceInfoConnector" should {

    "generate the correct url" in {
      connector.btaUrl shouldBe "/business-account/partial/service-info"
    }
  }

  "getServiceInfoPartial" when {

    "a connectionExceptionsAsHtmlPartialFailure error is returned" should {

      "return the fall back partial" in {
        mockConnectorCall(Future.successful(Failure(Some(Status.GATEWAY_TIMEOUT))))
        await(connector.getServiceInfoPartial(fakeRequest, ec)) shouldBe btaNavigationLinks()
      }
    }

    "an unexpected Exception is returned" should {

      "return the fall back partial" in {
        mockConnectorCall(Future.successful(Failure(Some(Status.INTERNAL_SERVER_ERROR))))
        await(connector.getServiceInfoPartial(fakeRequest, ec)) shouldBe btaNavigationLinks()
      }
    }

    "a successful response is returned" should {

      "return the Bta partial" in {
        mockConnectorCall(Future.successful(Success(None,validHtml)))
        await(connector.getServiceInfoPartial(fakeRequest, ec)) shouldBe validHtml
      }
    }
  }
}
