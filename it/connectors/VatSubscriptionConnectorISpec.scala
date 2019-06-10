/*
 * Copyright 2019 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.IntegrationBaseSpec
import models.CustomerInformation
import models.errors.ServerSideError
import stubs.CustomerInfoStub
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class VatSubscriptionConnectorISpec extends IntegrationBaseSpec {

  private trait Test {
    def setupStubs(): StubMapping

    val connector: VatSubscriptionConnector = app.injector.instanceOf[VatSubscriptionConnector]
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "Calling getCustomerInfo" when {

    "the API returns a valid response" should {

      "provide a user's information" in new Test {
        override def setupStubs(): StubMapping = CustomerInfoStub.stubCustomerInfo()

        setupStubs()
        val expected = Right(CustomerInformation(
          Some("Cheapo Clothing Ltd"),
          Some("Vincent"),
          Some("Vatreturn"),
          Some("Cheapo Clothing"),
          hasFlatRateScheme = true,
          Some(true)
        ))
        private val result = await(connector.getCustomerInfo("999999999"))

        result shouldBe expected
      }
    }

    "the API returns an error" should {

      val message: String = """{"code":"500","message":"INTERNAL_SERVER_ERROR"}"""

      "return an error" in new Test {
        override def setupStubs(): StubMapping = CustomerInfoStub.stubErrorFromApi
        setupStubs()
        val expected = Left(ServerSideError("500", message))
        private val result = await(connector.getCustomerInfo("999999999"))

        result shouldBe expected
      }
    }
  }

}
