/*
 * Copyright 2023 HM Revenue & Customs
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

package test.connectors

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.MandationStatuses.mtdfb
import connectors.VatSubscriptionConnector
import models.CustomerInformation
import models.errors.ServerSideError
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import test.helpers.IntegrationBaseSpec
import test.stubs.CustomerInfoStub
import uk.gov.hmrc.http.HeaderCarrier

class VatSubscriptionConnectorISpec extends IntegrationBaseSpec {

  private trait Test {
    def setupStubs(): StubMapping

    val connector: VatSubscriptionConnector = app.injector.instanceOf[VatSubscriptionConnector]
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "Calling getCustomerInfo" when {

    "the API returns a valid response" should {

      "provide a user's information" in new Test {
        override def setupStubs(): StubMapping = CustomerInfoStub.stubCustomerInfo

        setupStubs()
        val expected = Right(CustomerInformation(
          Some("Cheapo Clothing Ltd"),
          Some("Vincent"),
          Some("Vatreturn"),
          Some("Cheapo Clothing"),
          isInsolvent = false,
          Some(true),
          insolvencyType = Some("01"),
          insolvencyDate = Some("2018-01-01"),
          hasFlatRateScheme = true,
          isPartialMigration = true,
          Some("2018-01-01"),
          Some("2018-01-01"),
          Some("2018-01-01"),
          mtdfb
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
