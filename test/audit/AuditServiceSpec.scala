/*
 * Copyright 2024 HM Revenue & Customs
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
import audit.models.ViewOpenVatObligationsAuditModel
import controllers.ControllerBaseSpec
import models.{User, VatReturnObligation}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.{ExecutionContext, Future}
import java.time.LocalDate


class AuditServiceSpec extends ControllerBaseSpec with BeforeAndAfterEach {

  private trait Test {

    lazy val mockAuditConnector: AuditConnector = mock[AuditConnector]

    def setupMocks(): Unit= {}

    def target(): AuditingService = {
      setupMocks()
      new AuditingService(mockConfig, mockAuditConnector)
    }

    "auditing a sequence of vat return obligations" should {

      "extract the data and pass it into the AuditConnector" in new Test {

        val testModel = ViewOpenVatObligationsAuditModel(
          User("111111111", hasNonMtdVat = true),
          Seq(VatReturnObligation(
            LocalDate.parse("2017-01-01"),
            LocalDate.parse("2017-12-31"),
            LocalDate.parse("2018-01-31"),
            "O",
            None,
            "#001"
          ))
        )

        override def setupMocks(): Unit = {
          super.setupMocks()

          (mockAuditConnector.sendEvent(_: DataEvent)(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *)
            .returns(Future.successful(AuditResult.Success))
        }

        target().extendedAudit(testModel, "localhost")
      }
    }
  }
}
