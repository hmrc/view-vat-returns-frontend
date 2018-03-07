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

package models

import java.time.LocalDate
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class VatReturnObligation(start: LocalDate,
                               end: LocalDate,
                               due: LocalDate,
                               status: String,
                               received: Option[LocalDate],
                               periodKey: String) extends Obligation

object VatReturnObligation {

  implicit val vatReturnObligationWrites: Writes[VatReturnObligation] = Json.writes[VatReturnObligation]

  implicit val vatReturnObligationReads: Reads[VatReturnObligation] = (
    (JsPath \ "start").read[LocalDate] and
    (JsPath \ "end").read[LocalDate] and
    (JsPath \ "due").read[LocalDate] and
    (JsPath \ "status").read[String] and
    (JsPath \ "received").readNullable[LocalDate] and
    (JsPath \ "periodKey").read[String]
  ) (VatReturnObligation.apply _)

  object Status extends Enumeration {
    val All: Status.Value = Value("A")
    val Outstanding: Status.Value = Value("O")
    val Fulfilled: Status.Value = Value("F")
  }
}
