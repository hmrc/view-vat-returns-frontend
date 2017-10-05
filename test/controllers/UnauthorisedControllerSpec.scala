/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import config.AppConfig
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.test.Helpers._

class UnauthorisedControllerSpec extends ControllerBaseSpec {

  "Calling the show action" should {

    lazy val messages = fakeApplication.injector.instanceOf[MessagesApi]
    lazy val mockAppConfig = fakeApplication.injector.instanceOf[AppConfig]

    lazy val target = new UnauthorisedController(messages, mockAppConfig)

    lazy val result = target.show(FakeRequest())

    "return 200 OK" in {
      status(result) shouldBe OK
    }

    "return HTML" in {
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }

  }

}
