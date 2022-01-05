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

package app

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

class RouteSpec extends AnyWordSpecLike with Matchers with GuiceOneAppPerSuite {

  "The route for the submitted 9 box returns via the returns page route" should {
    "be /vat-through-software/vat-returns/submitted/2018/%23001" in {
      controllers.routes.ReturnsController.vatReturn(2018, "#001").url shouldBe
        "/vat-through-software/vat-returns/submitted/2018/%23001"
    }
  }

  "The route for the submitted 9 box returns via the payments page route" should {
    "be /vat-through-software/vat-returns/%23001" in {
      controllers.routes.ReturnsController.vatReturnViaPayments("#001").url shouldBe
        "/vat-through-software/vat-returns/%23001"
    }
  }

  "The route for the list of returns" should {
    "be /vat-through-software/vat-returns/submitted" in {
      controllers.routes.SubmittedReturnsController.submittedReturns.url shouldBe "/vat-through-software/vat-returns/submitted"
    }
  }

  "The route for the return deadlines" should {
    "be /vat-through-software/vat-returns/return-deadlines" in {
      controllers.routes.ReturnDeadlinesController.returnDeadlines.url shouldBe "/vat-through-software/vat-returns/return-deadlines"
    }
  }
}
