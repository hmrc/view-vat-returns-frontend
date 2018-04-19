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

package auditSpec

import audit.AuditingService
import audit.models.PaymentAuditModel
import config.FrontendAuditConnector
import controllers.ControllerBaseSpec
import models.User
import models.payments.PaymentDetailsModel
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuditServiceSpec extends ControllerBaseSpec with BeforeAndAfterEach {

  private trait Test {

    lazy val mockAuditConnector: FrontendAuditConnector = mock[FrontendAuditConnector]
    implicit val hc: HeaderCarrier = HeaderCarrier()

    def setupMocks(): Unit= {}

    def target(): AuditingService = {
      setupMocks()
      new AuditingService(mockConfig, mockAuditConnector)
    }

    "auditing a payment hand off" when {

      "payment details have been supplied" should {
        "extract the data and pass it into the AuditConnector" in new Test {

          val testModel = PaymentAuditModel(
            User("111111111", true, true),
            PaymentDetailsModel(
              taxType = "vat",
              taxReference = "123456789",
              amountInPence = 99,
              taxPeriodMonth = 1,
              taxPeriodYear = 2018,
              returnUrl = "/return-url",
              backUrl = "/back-url",
              periodKey = "ABCD"
            ),
            "/return-page-url"
          )

          override def setupMocks(): Unit = {
            super.setupMocks()

            (mockAuditConnector.sendEvent(_: DataEvent)(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(Future.successful(AuditResult.Success))
          }

          target.audit(testModel, controllers.feedback.routes.FeedbackController.show().url)
        }
      }

    }
  }

}
