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

package config

object ConfigKeys {

  val contactFrontendHost: String = "contact-frontend.host"
  val contactFrontendService: String = "contact-frontend"

  private val googleAnalyticsRoot: String = "google-analytics"
  val googleAnalyticsToken: String = googleAnalyticsRoot + ".token"
  val googleAnalyticsHost: String = googleAnalyticsRoot + ".host"

  val whitelistEnabled: String = "whitelist.enabled"
  val whitelistedIps: String = "whitelist.allowedIps"
  val whitelistExcludedPaths: String = "whitelist.excludedPaths"
  val whitelistShutterPage: String = "whitelist.shutter-page-url"

  val signInBaseUrl: String = "signIn.url"

  val userResearchBannerFeature: String = "features.userResearchBanner.enabled"
  val staticDateEnabledFeature: String = "features.staticDate.enabled"
  val staticDateValue: String = "date-service.staticDate.value"
  val future2020DateValue: String = "date-service.futureDate.value"
  val useVatReturnsService: String = "features.useVatReturnsService.enabled"
  val useVatObligationsService: String = "features.useVatObligationsService.enabled"
  val future2020DateEnabledFeature: String = "features.futureDate.enabled"
  val useLanguageSelectorFeature: String = "features.useLanguageSelector.enabled"
  val submitReturnFeatures: String = "features.submitReturnFeatures.enabled"
  val agentAccessFeature: String = "features.agentAccess.enabled"

  val finalReturnPeriodKey: String = "final-return.periodKey"

  val businessTaxAccountBase: String = "business-tax-account"
  val businessTaxAccountHost: String = "business-tax-account.host"
  val businessTaxAccountUrl: String = "business-tax-account.homeUrl"
  val businessTaxAccountMessagesUrl: String = "business-tax-account.messagesUrl"
  val businessTaxAccountManageAccountUrl: String = "business-tax-account.manageAccountUrl"

  val helpAndContactFrontendBase: String = "help-and-contact-frontend.host"
  val helpAndContactHelpUrl: String = "help-and-contact-frontend.helpUrl"

  val vatObligations: String = "vat-obligations"

  val vatSummaryBase: String = "vat-summary-frontend.host"
  val vatDetailsUrl: String = "vat-summary-frontend.detailsUrl"
  val vatPaymentsUrl: String = "vat-summary-frontend.paymentsUrl"

  val submitVatReturnBase: String = "submit-vat-return-frontend.host"
  val submitVatReturnUrl: String = "submit-vat-return-frontend.url"
  val submitVatReturnForm: String = "submit-vat-return-frontend.submit-form"

  val vatAgentClientLookupFrontendHost: String = "vat-agent-client-lookup-frontend.host"
  val vatAgentClientLookupFrontendUrl: String = "vat-agent-client-lookup-frontend.startUrl"
  val vatAgentClientLookupActionUrl: String = "vat-agent-client-lookup-frontend.actionUrl"
  val vatAgentClientLookupUnauthorisedUrl: String = "vat-agent-client-lookup-frontend.unauthorisedUrl"

  val reportVatErrorUrl: String = "reportVatErrorUrl"

  val governmentGatewayHost: String = "government-gateway.host"

  val surveyHost: String = "feedback-frontend.host"
  val surveyUrl: String = "feedback-frontend.url"

  val mtdVatSignUpBaseUrl: String = "vat-subscription-frontend.host"
  val mtdVatSignUpUrl: String = "vat-subscription-frontend.signUpUrl"

  val selfHost: String = "self.host"

  val timeoutPeriod: String = "timeout.period"
  val timeoutCountDown: String = "timeout.countDown"

  val govUkCommercialSoftwareUrl: String = "govuk-commercial-software.url"

  val host: String = "host"
}
