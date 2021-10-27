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

package mocks

import config.AppConfig
import config.features.Features
import play.api.i18n.Lang
import play.api.{Configuration, Mode}
import play.api.mvc.Call

class MockAppConfig(val runModeConfiguration: Configuration, val mode: Mode = Mode.Test) extends AppConfig {

  override def feedbackUrl(redirect: String): String = "localhost/feedback"
  override val appName: String = "view-vat-returns-frontend"
  override val reportAProblemPartialUrl: String = ""
  override val reportAProblemNonJSUrl: String = ""
  override val signInUrl: String = ""
  override val features: Features = new Features(runModeConfiguration)
  override val portalUrl: String => String = (vrn: String) => s"/portal-url/$vrn"
  override val vatApiBaseUrl: String = ""
  override val vatReturnsBaseUrl: String = "/return-api"
  override val vatObligationsBaseUrl: String = "/obligations-api"
  override val financialDataBaseUrl: String = ""
  override val btaBaseUrl: String = ""
  override val btaHomeUrl = "bta-url"
  override val vatDetailsUrl = "vat-details-url"
  override val vatPaymentsUrl: String = "vat-payments-url"
  override val reportVatErrorUrl: String = "report-vat-error-url"
  override val feedbackFormPartialUrl: String = "BasefeedbackUrl"
  override val contactFormServiceIdentifier: String = "VATVC"
  override val staticDateValue: String = "2018-05-01"
  override val finalReturnPeriodKey: String = "9999"
  override val accessibilityLinkUrl: String = "/accessibility-statement"
  override def surveyUrl(identifier: String): String = s"/some-survey-url/$identifier"
  override def signOutUrl(identifier: String): String = s"/some-gg-signout-url/$identifier"
  override val mtdVatSignUpUrl: String = "mtd-sign-up"
  override val unauthorisedSignOutUrl: String = "/unauth-signout"
  override val vatSubscriptionBaseUrl: String = ""
  override val selfHost: String = "www.app.com"
  override val timeoutPeriod: Int = 1800
  override val timeoutCountdown: Int = 20
  override val govUkCommercialSoftwareUrl: String =
    "https://www.gov.uk/guidance/software-for-sending-income-tax-updates"
  override val languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )
  override val gtmContainer: String = "x"
  override val routeToSwitchLanguage: String => Call =
    (lang: String) => controllers.routes.LanguageController.switchLanguage(lang)
  override val submitVatReturnBase: String = "submitReturnBase"
  override val submitVatReturnUrl: String = submitVatReturnBase + "/submitUrl"
  override val submitVatReturnForm: String => String = periodKey => submitVatReturnUrl + s"$periodKey/submit-form"
  override val submitVatHonestyDeclaration: String => String = periodKey => submitVatReturnUrl + s"$periodKey/honesty-declaration"
  override val agentClientLookupUrl: String => String = uri =>  s"/agent-client-lookup/$uri"
  override val agentClientUnauthorisedUrl: String => String = uri => s"agent-client-unauthorised/$uri"
  override val agentClientHubUrl: String = "agent-client-agent-action"
  override val directDebitInterruptUrl: String = "/directDebitUrl"
}
