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

package services

import java.time.LocalDate
import javax.inject.Inject

class DateService @Inject()(appConfig: config.AppConfig) {

  def now(): LocalDate = {

    val staticDateEnabled: Boolean = appConfig.features.staticDateEnabled()
    val future2020DateEnabled: Boolean = appConfig.features.future2020DateEnabled()

    (staticDateEnabled, future2020DateEnabled) match {
      case (true, false) => LocalDate.parse(appConfig.staticDateValue)
      case (false, true) => LocalDate.parse(appConfig.future2020DateValue)
      case (false, false) => LocalDate.now()
      case (true, true) => LocalDate.now()
    }
  }
}
