/*
 * Copyright 2017 HM Revenue & Customs
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

import java.time.LocalDate

import play.api.libs.json.{Format, Json}

case class VatReturn(
                    businessName: String,
                    startDate: LocalDate,
                    endDate: LocalDate,
                    dateSubmitted: LocalDate,
                    dueDate: LocalDate,
                    totalSales: Int,
                    euSales: Int,
                    vatChargedInUk: Int,
                    vatChargedToEu: Int,
                    totalCosts: Int,
                    euCosts: Int,
                    totalVatCharged: Int,
                    totalVatReclaimed: Int,
                    owedToHmrc: Int,
                    vatBalance: Int
                    )

object VatReturn {

  implicit val format: Format[VatReturn] = Json.format[VatReturn]
}