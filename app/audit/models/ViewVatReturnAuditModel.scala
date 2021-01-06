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

import models.{User, VatReturn}
import models.viewModels.VatReturnViewModel

case class ViewVatReturnAuditModel(user: User,
                                   vatReturnViewInfo: VatReturnViewModel) extends AuditModel {

  override val auditType: String = "VatReturnPageView"

  override val transactionName: String = "view-vat-return"

  private val vatReturn: VatReturn = vatReturnViewInfo.vatReturnDetails.vatReturn

  private val returnDetails: Map[String, String] = Map(
    "userDisplayName" -> vatReturnViewInfo.entityName.getOrElse("-"),
    "periodFrom" -> vatReturnViewInfo.periodFrom.toString,
    "periodTo" -> vatReturnViewInfo.periodTo.toString,
    "dueDate" -> vatReturnViewInfo.dueDate.toString,
    "outstandingAmount" -> vatReturnViewInfo.returnTotal.toString(),
    "periodKey" -> vatReturn.periodKey,
    "vatDueSales" -> vatReturn.vatDueSales.toString,
    "vatDueAcquisitions" -> vatReturn.vatDueAcquisitions.toString,
    "totalVatDue" -> vatReturn.totalVatDue.toString,
    "vatReclaimedCurrentPeriod" -> vatReturn.vatReclaimedCurrentPeriod.toString,
    "netVatDue" -> vatReturn.netVatDue.toString,
    "totalValueSalesExVAT" -> vatReturn.totalValueSalesExVAT.toString,
    "totalValuePurchasesExVAT" -> vatReturn.totalValuePurchasesExVAT.toString,
    "totalValueGoodsSuppliedExVAT" -> vatReturn.totalValueGoodsSuppliedExVAT.toString,
    "totalAcquisitionsExVAT" -> vatReturn.totalAcquisitionsExVAT.toString
  )

  override val detail: Map[String, String] = Map("vrn" -> user.vrn) ++ returnDetails

}
