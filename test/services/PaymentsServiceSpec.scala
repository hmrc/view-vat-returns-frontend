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

package services

import connectors.PaymentsConnector
import connectors.httpParsers.ResponseHttpParsers.HttpPostResult
import models.errors.UnknownError
import models.payments.PaymentDetailsModel
import org.scalamock.matchers.Matchers
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class PaymentsServiceSpec extends UnitSpec with MockFactory with Matchers {

  "Calling the .setupJourney method" when {

    trait Test {

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val mockPaymentsConnector: PaymentsConnector = mock[PaymentsConnector]

      def setup(): Any

      def target: PaymentsService = {
        setup()
        new PaymentsService(mockPaymentsConnector)
      }
    }

    val amountInPence = 123456
    val taxPeriodMonth = 2
    val taxPeriodYear = 2018

    val paymentDetails = PaymentDetailsModel("vat",
      "123456789",
      amountInPence,
      taxPeriodMonth,
      taxPeriodYear,
      "http://domain/path",
      "http://domain/return-path",
      "#001"
    )

    "the connector is successful" should {

      "return a redirect url" in new Test {

        val expectedRedirectUrl = "http://www.google.com"
        val expectedResult: HttpPostResult[String] = Right(expectedRedirectUrl)

        override def setup(): Any = {
          (mockPaymentsConnector.setupJourney(_: PaymentDetailsModel)(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *)
            .returns(Right(expectedRedirectUrl))
        }


        val result: String = await(target.setupPaymentsJourney(paymentDetails))

        result shouldBe expectedRedirectUrl
      }
    }

    "the connector is unsuccessful" should {

      "throw an exception" in new Test {

        val expectedResult: HttpPostResult[String] = Left(UnknownError)

        override def setup(): Any = {
          (mockPaymentsConnector.setupJourney(_: PaymentDetailsModel)(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *)
            .returns(expectedResult)
        }

        the[Exception] thrownBy {
          await(target.setupPaymentsJourney(paymentDetails))
        } should have message "Received an unknown error."
      }
    }
  }
}
