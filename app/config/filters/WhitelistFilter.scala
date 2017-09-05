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

package config.filters

import javax.inject.Inject

import config.AppConfig
import play.api.Application
import uk.gov.hmrc.whitelist.AkamaiWhitelistFilter

class WhitelistFilter @Inject()(app: Application) extends AkamaiWhitelistFilter {

  private lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  override lazy val whitelist: Seq[String] = appConfig.ip-whitelist.urls

}
