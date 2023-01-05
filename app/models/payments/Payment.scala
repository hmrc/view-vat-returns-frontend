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

package models.payments

import java.time.LocalDate

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class Payment(chargeType: String,
                   periodFrom: LocalDate,
                   periodTo: LocalDate,
                   due: LocalDate,
                   outstandingAmount: BigDecimal,
                   periodKey: String)

object Payment {

  private def createPayment(chargeType: String,
                            periodFrom: LocalDate,
                            periodTo: LocalDate,
                            due: LocalDate,
                            outstandingAmount: Option[BigDecimal],
                            periodKey: String): Payment = {
    Payment(chargeType, periodFrom, periodTo, due, outstandingAmount.getOrElse(0), periodKey)
  }

  implicit val paymentReads: Reads[Payment] = (
    (JsPath \ "chargeType").read[String] and
      (JsPath \ "taxPeriodFrom").read[LocalDate] and
      (JsPath \ "taxPeriodTo").read[LocalDate] and
      (JsPath \ "items") (0).\("dueDate").read[LocalDate] and
      (JsPath \ "outstandingAmount").readNullable[BigDecimal] and
      (JsPath \ "periodKey").read[String]
    ) (Payment.createPayment _)

}
