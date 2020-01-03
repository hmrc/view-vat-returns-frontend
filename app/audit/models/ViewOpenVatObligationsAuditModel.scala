/*
 * Copyright 2020 HM Revenue & Customs
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

case class ViewOpenVatObligationsAuditModel(user: User,
                                            obligations: Seq[VatReturnObligation]) extends ExtendedAuditModel {

  override val auditType: String = "ReturnDeadlinesPageView"

  override val transactionName: String = "view-open-vat-obligations"

  private case class OpenObligationDetails(periodFrom: LocalDate,
                                           periodTo: LocalDate,
                                           due: LocalDate,
                                           periodKey: String)

  private implicit val auditPaymentsWrites: Writes[OpenObligationDetails] = Json.writes[OpenObligationDetails]

  private case class AuditDetail(vrn: String, openObligations: Seq[OpenObligationDetails])

  private implicit val auditDetailWrites: Writes[AuditDetail] = Json.writes[AuditDetail]

  private val openObligations = obligations.map { obligation =>
    OpenObligationDetails(obligation.periodFrom, obligation.periodTo, obligation.due, obligation.periodKey)
  }

  private val eventData = AuditDetail(user.vrn, openObligations)
  override val detail: JsValue = Json.toJson(eventData)

}
