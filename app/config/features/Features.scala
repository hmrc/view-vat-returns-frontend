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

package config.features

import javax.inject.{Inject, Singleton}

import config.ConfigKeys
import play.api.Configuration

@Singleton
class Features @Inject()(config: Configuration) {
  val userResearchBanner = new Feature(ConfigKeys.userResearchBannerFeature, config)
  val staticDateEnabled = new Feature(ConfigKeys.staticDateEnabledFeature, config)
  val enableVatReturnsService = new Feature(ConfigKeys.useVatReturnsService, config)
  val enableVatObligationsService = new Feature(ConfigKeys.useVatObligationsService, config)
  val future2020DateEnabled = new Feature(ConfigKeys.future2020DateEnabledFeature, config)
  val useLanguageSelector = new Feature(ConfigKeys.useLanguageSelectorFeature, config)
  val submitReturnFeatures = new Feature(ConfigKeys.submitReturnFeatures, config)
}
