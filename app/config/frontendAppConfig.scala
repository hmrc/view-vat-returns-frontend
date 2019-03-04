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

import java.util.Base64

import javax.inject.{Inject, Singleton}
import config.features.Features
import play.api.Mode.Mode
import play.api.mvc.Call
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.config.ServicesConfig
import config.{ConfigKeys => Keys}
import play.api.i18n.Lang

trait AppConfig extends ServicesConfig {
  val appName:String
  val analyticsToken: String
  val analyticsHost: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val whitelistEnabled: Boolean
  val whitelistedIps: Seq[String]
  val whitelistExcludedPaths: Seq[Call]
  val shutterPage: String
  val signInUrl: String
  val features: Features
  val portalUrl: String => String
  val vatApiBaseUrl: String
  val vatReturnsBaseUrl: String
  val vatObligationsBaseUrl: String
  val vatSubscriptionBaseUrl: String
  val financialDataBaseUrl: String
  val btaHomeUrl: String
  val vatDetailsUrl: String
  val reportVatErrorUrl: String
  val feedbackFormPartialUrl: String
  val contactFormServiceIdentifier: String
  val staticDateValue: String
  val future2020DateValue: String
  val finalReturnPeriodKey: String
  val surveyUrl: String
  val signOutUrl: String
  val mtdVatSignUpUrl: String
  val unauthorisedSignOutUrl: String
  val vatPaymentsUrl: String
  val selfHost: String
  val timeoutPeriod: Int
  val timeoutCountdown: Int
  val govUkCommercialSoftwareUrl: String
  val languageMap: Map[String, Lang]
}

@Singleton
class FrontendAppConfig @Inject()(val runModeConfiguration: Configuration, val environment: Environment) extends AppConfig {

  override val mode: Mode = environment.mode

  override val appName: String = getString("appName")
  private lazy val contactHost: String = getString(Keys.contactFrontendHost)
  override lazy val contactFormServiceIdentifier: String = "VATVC"
  private lazy val contactFrontendService = baseUrl(Keys.contactFrontendService)
  override lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override lazy val feedbackFormPartialUrl: String = s"$contactFrontendService/contact/beta-feedback/form"

  override lazy val analyticsToken: String = getString(Keys.googleAnalyticsToken)
  override lazy val analyticsHost: String = getString(Keys.googleAnalyticsHost)

  private def whitelistConfig(key: String): Seq[String] = Some(new String(Base64.getDecoder
    .decode(runModeConfiguration.getString(key).getOrElse("")), "UTF-8"))
    .map(_.split(",")).getOrElse(Array.empty).toSeq

  override lazy val whitelistEnabled: Boolean = runModeConfiguration.getBoolean(Keys.whitelistEnabled).getOrElse(true)
  override lazy val whitelistedIps: Seq[String] = whitelistConfig(Keys.whitelistedIps)
  override lazy val whitelistExcludedPaths: Seq[Call] = whitelistConfig(Keys.whitelistExcludedPaths).map(path => Call("GET", path))
  override lazy val shutterPage: String = getString(Keys.whitelistShutterPage)

  private lazy val signInBaseUrl: String = getString(Keys.signInBaseUrl)
  private lazy val signInContinueUrl: String = ContinueUrl(vatDetailsUrl).encodedUrl
  private lazy val signInOrigin = getString("appName")
  override lazy val signInUrl: String = s"$signInBaseUrl?continue=$signInContinueUrl&origin=$signInOrigin"

  override val features = new Features(runModeConfiguration)

  override val portalUrl: String => String = (vrn: String) => s"/vat-file/trader/$vrn/periods"
  override val vatApiBaseUrl: String = baseUrl("vat-api")
  override val vatReturnsBaseUrl: String = baseUrl("vat-returns")
  override lazy val vatObligationsBaseUrl: String = baseUrl(Keys.vatObligations)
  override val vatSubscriptionBaseUrl: String = baseUrl("vat-subscription")
  override val financialDataBaseUrl: String = baseUrl("financial-transactions")

  override lazy val btaHomeUrl: String = getString(Keys.businessTaxAccountBase) + getString(Keys.businessTaxAccountUrl)

  private lazy val vatSummaryBase: String = getString(Keys.vatSummaryBase)
  override lazy val vatDetailsUrl: String = vatSummaryBase + getString(Keys.vatDetailsUrl)
  override lazy val vatPaymentsUrl: String = vatSummaryBase + getString(Keys.vatPaymentsUrl)

  override lazy val reportVatErrorUrl: String = getString(Keys.reportVatErrorUrl)

  override lazy val staticDateValue: String = getString(Keys.staticDateValue)
  override lazy val future2020DateValue: String = getString(Keys.future2020DateValue)

  override lazy val finalReturnPeriodKey: String = getString(Keys.finalReturnPeriodKey)

  private lazy val surveyBaseUrl: String = getString(Keys.surveyHost) + getString(Keys.surveyUrl)
  override lazy val surveyUrl: String = s"$surveyBaseUrl/?origin=$contactFormServiceIdentifier"

  private lazy val governmentGatewayHost: String = getString(Keys.governmentGatewayHost)

  override lazy val signOutUrl: String = s"$governmentGatewayHost/gg/sign-out?continue=$surveyUrl"
  override lazy val unauthorisedSignOutUrl: String = s"$governmentGatewayHost/gg/sign-out?continue=$signInContinueUrl"

  private val mtdVatSignUpBaseUrl: String = getString(Keys.mtdVatSignUpBaseUrl)
  override lazy val mtdVatSignUpUrl: String = mtdVatSignUpBaseUrl + getString(Keys.mtdVatSignUpUrl)

  override lazy val selfHost: String = getString(Keys.selfHost)

  override lazy val timeoutPeriod: Int = getString(Keys.timeoutPeriod).toInt
  override lazy val timeoutCountdown: Int = getString(Keys.timeoutCountDown).toInt

  override lazy val govUkCommercialSoftwareUrl: String = getString(Keys.govUkCommercialSoftwareUrl)

  override val languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )
}
