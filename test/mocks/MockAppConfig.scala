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

package mocks

import config.AppConfig
import config.features.Features
import play.api.Mode.Mode
import play.api.{Configuration, Mode}
import play.api.mvc.Call

class MockAppConfig(val runModeConfiguration: Configuration, val mode: Mode = Mode.Test) extends AppConfig {
  override val appName: String = "view-vat-returns-frontend"
  override val analyticsToken: String = ""
  override val analyticsHost: String = ""
  override val reportAProblemPartialUrl: String = ""
  override val reportAProblemNonJSUrl: String = ""
  override val whitelistEnabled: Boolean = false
  override val whitelistedIps: Seq[String] = Seq("")
  override val whitelistExcludedPaths: Seq[Call] = Nil
  override val shutterPage: String = "https://www.tax.service.gov.uk/shutter/vat-through-software"
  override val signInUrl: String = ""
  override val features: Features = new Features(runModeConfiguration)
  override val portalUrl: String => String = (vrn: String) => s"/portal-url/$vrn"
  override val vatApiBaseUrl: String = ""
  override val financialDataBaseUrl: String = ""
  override val btaHomeUrl = "bta-url"
  override val vatDetailsUrl = "vat-details-url"
  override val vatPaymentsUrl: String = "vat-payments-url"
  override val paymentsServiceUrl: String = "payments-url"
  override val setupPaymentsJourneyPath: String = "/payment/start"
  override val paymentsReturnUrl: String = "payments-return-url"
  override val reportVatErrorUrl: String = "report-vat-error-url"
  override val feedbackFormPartialUrl: String = "BasefeedbackUrl"
  override val contactFormServiceIdentifier: String = "VATVC"
  override val staticDateValue: String = "2018-05-01"
  override val surveyUrl: String = "/some-survey-url"
  override val signOutUrl: String = "/some-gg-signout-url"
  override val mtdVatSignUpUrl: String = "mtd-sign-up"
  override val unauthorisedSignOutUrl: String = ""
  override val vatSubscriptionBaseUrl: String = ""
  override val selfHost: String = "www.app.com"
  override val timeoutPeriod: Int = 1800
  override val timeoutCountdown: Int = 20
}

