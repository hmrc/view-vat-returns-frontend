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

import java.time.LocalDate

import connectors.httpParsers.VatReturnHttpParser.HttpGetResult
import connectors.VatApiConnector
import controllers.ControllerBaseSpec
import models.User
import models.VatReturn
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{ExecutionContext, Future}

class ReturnsServiceSpec extends ControllerBaseSpec {

  private trait Test {
    val exampleVatReturn: VatReturn = VatReturn(
      LocalDate.parse("2017-01-01"),
      LocalDate.parse("2017-03-31"),
      LocalDate.parse("2017-04-06"),
      LocalDate.parse("2017-04-08"),
      1297,
      5755,
      7052,
      5732,
      1320,
      77656,
      765765,
      55454,
      545645
    )
    val mockConnector: VatApiConnector = mock[VatApiConnector]
    val service = new ReturnsService(mockConnector)
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "Calling .getVatReturn" should {

    "return a VAT Return" in new Test {
      (mockConnector.getVatReturnDetails(_: String, _: String)(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(Future.successful(Right(exampleVatReturn)))

      lazy val result: HttpGetResult[VatReturn] = await(service.getVatReturnDetails(User("999999999"), "periodKey"))

      result shouldBe Right(exampleVatReturn)
    }
  }
}
