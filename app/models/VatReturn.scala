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

package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class VatReturn(periodKey: String,
                     vatDueSales: BigDecimal, // Box 1
                     vatDueAcquisitions: BigDecimal, // Box 2
                     totalVatDue: BigDecimal, // Box 3
                     vatReclaimedCurrentPeriod: BigDecimal, // Box 4
                     netVatDue: BigDecimal, // Box 5
                     totalValueSalesExVAT: BigDecimal, // Box 6
                     totalValuePurchasesExVAT: BigDecimal, // Box 7
                     totalValueGoodsSuppliedExVAT: BigDecimal, // Box 8
                     totalAcquisitionsExVAT: BigDecimal)   // Box 9

object VatReturn {

  implicit val vatReturnWrites: Writes[VatReturn] = (
    (JsPath \ "periodKey").write[String] and
      (JsPath \ "vatDueSales").write[BigDecimal] and
      (JsPath \ "vatDueAcquisitions").write[BigDecimal] and
      (JsPath \ "totalVatDue").write[BigDecimal] and
      (JsPath \ "vatReclaimedCurrPeriod").write[BigDecimal] and
      (JsPath \ "netVatDue").write[BigDecimal] and
      (JsPath \ "totalValueSalesExVAT").write[BigDecimal] and
      (JsPath \ "totalValuePurchasesExVAT").write[BigDecimal] and
      (JsPath \ "totalValueGoodsSuppliedExVAT").write[BigDecimal] and
      (JsPath \ "totalAcquisitionsExVAT").write[BigDecimal]
    ) (unlift(VatReturn.unapply))

  implicit val vatReturnReads: Reads[VatReturn] = (
    (JsPath \ "periodKey").read[String] and
    (JsPath \ "vatDueSales").read[BigDecimal] and
    (JsPath \ "vatDueAcquisitions").read[BigDecimal] and
    (JsPath \ "totalVatDue").read[BigDecimal] and
    (JsPath \ "vatReclaimedCurrPeriod").read[BigDecimal] and
    (JsPath \ "netVatDue").read[BigDecimal] and
    (JsPath \ "totalValueSalesExVAT").read[BigDecimal] and
    (JsPath \ "totalValuePurchasesExVAT").read[BigDecimal] and
    (JsPath \ "totalValueGoodsSuppliedExVAT").read[BigDecimal] and
    (JsPath \ "totalAcquisitionsExVAT").read[BigDecimal]
    ) (VatReturn.apply _)
}
