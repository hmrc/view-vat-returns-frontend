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
import models.errors.{ApiSingleError, BadRequestError, MultipleErrors}
import models.VatReturn
import play.api.libs.json.Json
import stubs.VatReturnsStub
import uk.gov.hmrc.http.HeaderCarrier

class VatReturnsConnectorISpec extends IntegrationBaseSpec {

  private trait Test {
    def setupStubs(): StubMapping

    val connector: VatReturnsConnector = app.injector.instanceOf[VatReturnsConnector]
    implicit val hc: HeaderCarrier = HeaderCarrier()
    val backendFeatureEnabled: Boolean =
      app.configuration.underlying.getBoolean("features.useVatReturnsService.enabled")
    val returnsStub = new VatReturnsStub(backendFeatureEnabled)
  }

  "Calling getReturnDetails" when {

    "the supplied data is all correct" should {

      "return a VatReturn" in new Test {
        override def setupStubs(): StubMapping = returnsStub.stubSuccessfulVatReturn

        val expected = Right(VatReturn(
          "#001",
          100,
          100,
          200,
          100,
          100,
          500,
          500,
          500,
          500
        ))

        setupStubs()
        private val result = await(connector.getVatReturnDetails("123456789", "%23001"))

        result shouldBe expected
      }
    }

    "the supplied VRN is invalid" should {

      "return a BadRequestError" in new Test {
        override def setupStubs(): StubMapping = returnsStub.stubInvalidVrnForReturns

        val expected = Left(BadRequestError(
          code = "VRN_INVALID",
          errorResponse = ""
        ))

        setupStubs()
        private val result = await(connector.getVatReturnDetails("123456789", "%23001"))

        result shouldBe expected
      }
    }

    "the supplied period key is invalid" should {

      "return a BadRequestError" in new Test {
        override def setupStubs(): StubMapping = returnsStub.stubInvalidPeriodKey

        val expected = Left(BadRequestError(
          code = "PERIOD_KEY_INVALID",
          errorResponse = ""
        ))

        setupStubs()
        private val result = await(connector.getVatReturnDetails("123456789", "%23001"))

        result shouldBe expected
      }
    }

    "there are multiple errors" should {

      "return a MultipleErrors" in new Test {
        override def setupStubs(): StubMapping = returnsStub.stubMultipleErrors

        val errors = Seq(ApiSingleError("ERROR_1", "MESSAGE_1"), ApiSingleError("ERROR_2", "MESSAGE_2"))
        val expected = Left(MultipleErrors("BAD_REQUEST", Json.toJson(errors).toString()))
        setupStubs()
        private val result = await(connector.getVatReturnDetails("123456789", "%23001"))

        result shouldBe expected
      }
    }
  }
}