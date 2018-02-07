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

package app

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.play.test.UnitSpec

class RouteSpec extends UnitSpec with GuiceOneAppPerSuite {

  "The route for the submitted 9 box returns via the returns page route" should {
    "be /view-your-vat-returns/return/%23001" in {
      controllers.routes.ReturnsController.vatReturnDetails("#001", 2018).url shouldBe
        "/view-your-vat-returns/return/%23001?yearEnd=2018"
    }
  }

  "The route for the submitted 9 box returns via the payments page route" should {
    "be /view-your-vat-returns/payments/return" in {
      controllers.routes.ReturnsController.vatPaymentReturnDetails("#001", 2018).url shouldBe
        "/view-your-vat-returns/payments/return/%23001?yearEnd=2018"
    }
  }

  "The route for the list of returns" should {
    "be /view-your-vat-returns/returns" in {
      controllers.routes.ReturnObligationsController.completedReturns(2017).url shouldBe "/view-your-vat-returns/returns/2017"
    }
  }

  "The route for the return deadlines" should {
    "be /view-your-vat-returns/return-deadlines" in {
      controllers.routes.ReturnObligationsController.returnDeadlines().url shouldBe "/view-your-vat-returns/return-deadlines"
    }
  }
}
