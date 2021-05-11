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

package config.features

import javax.inject.{Inject, Singleton}

import config.ConfigKeys
import play.api.Configuration

@Singleton
class Features @Inject()(config: Configuration) {
  val userResearchBanner = new Feature(ConfigKeys.userResearchBannerFeature, config)
  val staticDateEnabled = new Feature(ConfigKeys.staticDateEnabledFeature, config)
  val useLanguageSelector = new Feature(ConfigKeys.useLanguageSelectorFeature, config)
  val submitReturnFeatures = new Feature(ConfigKeys.submitReturnFeatures, config)
  val agentAccess = new Feature(ConfigKeys.agentAccessFeature, config)
  val enablePrintPastReturns = new Feature(ConfigKeys.enablePrintPastReturns, config)
  val niProtocolEnabled = new Feature(ConfigKeys.niProtocolFeature, config)
}
