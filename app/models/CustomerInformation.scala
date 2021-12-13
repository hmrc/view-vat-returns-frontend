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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._
import java.time.LocalDate

case class CustomerInformation(organisationName: Option[String],
                               firstName: Option[String],
                               lastName: Option[String],
                               tradingName: Option[String],
                               isInsolvent: Boolean,
                               continueToTrade: Option[Boolean],
                               insolvencyType: Option[String],
                               insolvencyDate: Option[String],
                               hasFlatRateScheme: Boolean,
                               isPartialMigration: Boolean,
                               customerMigratedToETMPDate: Option[String],
                               hybridToFullMigrationDate: Option[String],
                               effectiveRegistrationDate: Option[String],
                               mandationStatus: String) {

  val entityName: Option[String] = (firstName, lastName, organisationName, tradingName) match {
    case (Some(firstName), Some(lastName), None, None) => Some(s"$firstName $lastName")
    case (None, None, organisationName, None) => organisationName
    case _ => tradingName
  }

  def extractDate: Option[String] = hybridToFullMigrationDate match {
    case Some(_) => hybridToFullMigrationDate
    case _ => customerMigratedToETMPDate
  }

  val exemptInsolvencyTypes = Seq("07", "12", "13", "14")
  val blockedInsolvencyTypes = Seq("08", "09", "10", "15")

  val isInsolventWithoutAccess: Boolean = (isInsolvent, insolvencyType) match {
    case (true, Some(inType)) if exemptInsolvencyTypes.contains(inType) => false
    case (true, Some(inType)) if blockedInsolvencyTypes.contains(inType) => true
    case (true, _) if continueToTrade.contains(false) => true
    case _ => false
  }

  def insolvencyDateFutureUserBlocked(today: LocalDate): Boolean =
    (isInsolvent, insolvencyType,insolvencyDate, continueToTrade) match {
      case (_, Some(inType),_,_) if exemptInsolvencyTypes.contains(inType) => false
      case (true, Some(_), Some(date), Some(true)) if LocalDate.parse(date).isAfter(today) => true
      case _ => false
    }
}

object CustomerInformation {

  implicit val customerInformationReads: Reads[CustomerInformation] = (
    (JsPath \ "customerDetails" \ "organisationName").readNullable[String] and
    (JsPath \ "customerDetails" \ "firstName").readNullable[String] and
    (JsPath \ "customerDetails" \ "lastName").readNullable[String] and
    (JsPath \ "customerDetails" \ "tradingName").readNullable[String] and
    (JsPath \ "customerDetails" \ "isInsolvent").read[Boolean] and
    (JsPath \ "customerDetails" \ "continueToTrade").readNullable[Boolean] and
    (JsPath \ "customerDetails" \ "insolvencyType").readNullable[String] and
    (JsPath \ "customerDetails" \ "insolvencyDate").readNullable[String] and
    (JsPath \ "flatRateScheme").readNullable[JsValue].map(_.isDefined) and
    (JsPath \\ "isPartialMigration").readNullable[Boolean].map(_.contains(true)) and
    (JsPath \\ "customerMigratedToETMPDate").readNullable[String] and
    (JsPath \\ "hybridToFullMigrationDate").readNullable[String] and
    (JsPath \ "customerDetails" \ "effectiveRegistrationDate").readNullable[String] and
    (JsPath \ "mandationStatus").read[String]
  )(CustomerInformation.apply _)
}
