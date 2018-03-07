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

package config

object ConfigKeys {

  val contactFrontendService: String = "contact-frontend.host"

  private val googleAnalyticsRoot: String = "google-analytics"
  val googleAnalyticsToken: String = googleAnalyticsRoot + ".token"
  val googleAnalyticsHost: String = googleAnalyticsRoot + ".host"

  val whitelistEnabled: String = "whitelist.enabled"
  val whitelistedIps: String = "whitelist.allowedIps"
  val whitelistExcludedPaths: String = "whitelist.excludedPaths"
  val whitelistShutterPage: String = "whitelist.shutter-page-url"

  val signInBaseUrl: String = "signIn.url"
  val signInContinueBaseUrl: String = "signIn.continueBaseUrl"

  val simpleAuthFeature: String = "features.simpleAuth.enabled"
  val userResearchBannerFeature: String = "features.userResearchBanner.enabled"
  val allowPaymentsFeature: String = "features.allowPayments.enabled"

  val businessTaxAccountBase: String = "business-tax-account.host"
  val businessTaxAccountUrl: String = "business-tax-account.homeUrl"

  val vatSummaryBase: String = "vat-summary-frontend.host"
  val vatDetailsUrl: String = "vat-summary-frontend.detailsUrl"
  val vatPaymentsUrl: String = "vat-summary-frontend.paymentsUrl"
  val vatMakePaymentUrl: String = "vat-summary-frontend.makePaymentUrl"

  val paymentsServiceBase: String = "payments-frontend.host"
  val paymentsServiceUrl: String = "payments-frontend.paymentUrl"
  val paymentsReturnBase: String = "payments-frontend.returnHost"
  val paymentsReturnUrl: String = "payments-frontend.returnUrl"
}
