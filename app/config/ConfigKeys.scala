/*
 * Copyright 2023 HM Revenue & Customs
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
  val contactFrontendService: String = "contact-frontend.serviceId"

  val signInBaseUrl: String = "signIn.url"

  val staticDateEnabledFeature: String = "features.staticDate.enabled"
  val staticDateValue: String = "date-service.staticDate.value"

  val finalReturnPeriodKey: String = "final-return.periodKey"

  val businessTaxAccount: String = "business-tax-account"
  val businessTaxAccountHost: String = "business-tax-account.host"
  val businessTaxAccountUrl: String = "business-tax-account.homeUrl"

  val vatObligations: String = "vat-obligations"

  val vatSummaryBase: String = "vat-summary-frontend.host"
  val vatDetailsUrl: String = "vat-summary-frontend.detailsUrl"
  val vatPaymentsUrl: String = "vat-summary-frontend.paymentsUrl"

  val submitVatReturnBase: String = "submit-vat-return-frontend.host"
  val submitVatReturnUrl: String = "submit-vat-return-frontend.url"
  val submitVatReturnForm: String = "submit-vat-return-frontend.submit-form"
  val submitVatHonestyDeclaration: String = "submit-vat-return-frontend.honesty-declaration"

  val vatAgentClientLookupFrontendHost: String = "vat-agent-client-lookup-frontend.host"
  val vatAgentClientLookupFrontendUrl: String = "vat-agent-client-lookup-frontend.startUrl"
  val vatAgentClientLookupHubUrl: String = "vat-agent-client-lookup-frontend.agentHubUrl"
  val vatAgentClientLookupUnauthorisedUrl: String = "vat-agent-client-lookup-frontend.unauthorisedUrl"

  val gtmContainer: String = "tracking-consent-frontend.gtm.container"

  val reportVatErrorUrl: String = "reportVatErrorUrl"

  val governmentGatewayHost: String = "government-gateway.host"

  val surveyHost: String = "feedback-frontend.host"
  val surveyUrl: String = "feedback-frontend.url"

  val selfHost: String = "self.host"

  val timeoutPeriod: String = "timeout.period"
  val timeoutCountDown: String = "timeout.countDown"

  val govUkCommercialSoftwareUrl: String = "govuk-commercial-software.url"

  val host: String = "host"
}
