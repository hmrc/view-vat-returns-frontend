/*
 * Copyright 2021 HM Revenue & Customs
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

package audit.models

import java.time.LocalDate

import models.{User, VatReturnObligation}
import play.api.libs.json.{JsValue, Json, Writes}

case class ViewSubmittedVatObligationsAuditModel(user: User,
                                                 obligations: Seq[VatReturnObligation]) extends ExtendedAuditModel {

  override val auditType: String = "SubmittedReturnsPageView"

  override val transactionName: String = "view-submitted-vat-obligations"

  private case class FulfilledObligationDetails(periodFrom: LocalDate,
                                                periodTo: LocalDate,
                                                due: LocalDate,
                                                periodKey: String)

  private implicit val auditPaymentsWrites: Writes[FulfilledObligationDetails] = Json.writes[FulfilledObligationDetails]

  private case class AuditDetail(vrn: String, fulfilledObligations: Seq[FulfilledObligationDetails])

  private implicit val auditDetailWrites: Writes[AuditDetail] = Json.writes[AuditDetail]

  private val fulfilledObligations = obligations.map { obligation =>
    FulfilledObligationDetails(obligation.periodFrom, obligation.periodTo, obligation.due, obligation.periodKey)
  }

  private val eventData = AuditDetail(user.vrn, fulfilledObligations)
  override val detail: JsValue = Json.toJson(eventData)

}
