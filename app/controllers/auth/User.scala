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

package controllers.auth

import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import common.Constants.MTD_VAT_ENROLMENT_KEY

case class User(enrolments: Enrolments) {

  // TODO clean this code when the identifier for the enrolment key is known
  lazy val mtdVatId: Option[String] = enrolments.enrolments.collectFirst {
    case Enrolment(MTD_VAT_ENROLMENT_KEY, EnrolmentIdentifier(_, value) :: _, _, _, _) => value
  }

}