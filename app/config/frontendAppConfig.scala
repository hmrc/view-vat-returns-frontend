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

import java.time.LocalDate
import java.util.Base64
import javax.inject.{Inject, Singleton}

import config.features.Features
import play.api.Mode.Mode
import play.api.mvc.Call
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.config.ServicesConfig
import config.{ConfigKeys => Keys}

trait AppConfig extends ServicesConfig {
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
  val portalUrl: String
  val vatApiBaseUrl: String
  val financialDataBaseUrl: String
  val btaHomeUrl: String
  val vatDetailsUrl: String
  val vatPaymentsUrl: String
}

@Singleton
class FrontendAppConfig @Inject()(val runModeConfiguration: Configuration, val environment: Environment) extends AppConfig {

  override val mode: Mode = environment.mode

  private lazy val contactHost: String = baseUrl(Keys.contactFrontendService)
  private lazy val contactFormServiceIdentifier: String = "MyService"

  override lazy val analyticsToken: String = getString(Keys.googleAnalyticsToken)
  override lazy val analyticsHost: String = getString(Keys.googleAnalyticsHost)
  override lazy val reportAProblemPartialUrl: String = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl: String = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  private def whitelistConfig(key: String): Seq[String] = Some(new String(Base64.getDecoder
    .decode(runModeConfiguration.getString(key).getOrElse("")), "UTF-8"))
    .map(_.split(",")).getOrElse(Array.empty).toSeq

  override lazy val whitelistEnabled: Boolean = runModeConfiguration.getBoolean(Keys.whitelistEnabled).getOrElse(true)
  override lazy val whitelistedIps: Seq[String] = whitelistConfig(Keys.whitelistedIps)
  override lazy val whitelistExcludedPaths: Seq[Call] = whitelistConfig(Keys.whitelistExcludedPaths).map(path => Call("GET", path))
  override lazy val shutterPage: String = getString(Keys.whitelistShutterPage)

  private lazy val signInBaseUrl: String = getString(Keys.signInBaseUrl)
  private lazy val signInContinueBaseUrl: String = runModeConfiguration.getString(Keys.signInContinueBaseUrl).getOrElse("")
  private lazy val signInContinueUrl: String = ContinueUrl(signInContinueBaseUrl +
    controllers.routes.ReturnObligationsController.completedReturns(LocalDate.now().getYear).url).encodedUrl
  private lazy val signInOrigin = getString("appName")
  override lazy val signInUrl: String = s"$signInBaseUrl?continue=$signInContinueUrl&origin=$signInOrigin"

  override val features = new Features(runModeConfiguration)

  override val portalUrl: String = ""
  override val vatApiBaseUrl: String = baseUrl("vat-api")
  override val financialDataBaseUrl: String = baseUrl("financial-transactions")

  private lazy val btaBaseUrl: String = getString(Keys.businessTaxAccountBase)
  override lazy val btaHomeUrl: String = btaBaseUrl + getString(Keys.businessTaxAccountUrl)

  private lazy val vatSummaryBase: String = getString(Keys.vatSummaryBase)
  override lazy val vatDetailsUrl: String = vatSummaryBase + getString(Keys.vatDetailsUrl)
  override lazy val vatPaymentsUrl: String = vatSummaryBase + getString(Keys.vatPaymentsUrl)
}