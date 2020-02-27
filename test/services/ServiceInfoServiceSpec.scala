/*
 * Copyright 2020 HM Revenue & Customs
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

package services

import connectors.ServiceInfoConnector
import controllers.ControllerBaseSpec
import play.api.mvc.Request
import play.twirl.api.Html

import scala.concurrent.{ExecutionContext, Future}

class ServiceInfoServiceSpec extends ControllerBaseSpec {

  val mockConnector: ServiceInfoConnector = mock[ServiceInfoConnector]
  val service: ServiceInfoService = new ServiceInfoService(mockConnector)

  val validHtml = Html("<nav>BTA LINK</nav>")

  ".getServiceInfoPartial" should {

    "return the HTML returned by the connector" in {
      (mockConnector.getServiceInfoPartial(_:Request[_], _:ExecutionContext))
        .expects(*, *)
        .returning(Future.successful(validHtml))

      val result: Html = await(service.getServiceInfoPartial(fakeRequest, ec))

      result shouldBe validHtml
    }
  }
}
