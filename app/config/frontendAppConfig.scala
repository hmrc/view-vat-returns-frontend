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

import java.net.URLEncoder
import config.features.Features
import config.{ConfigKeys => Keys}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait AppConfig {
  val appName:String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String
  val signInUrl: String
  val features: Features
  val portalUrl: String => String
  val vatApiBaseUrl: String
  val vatReturnsBaseUrl: String
  val vatObligationsBaseUrl: String
  val vatSubscriptionBaseUrl: String
  val financialDataBaseUrl: String
  val btaBaseUrl: String
  val btaHomeUrl: String
  val vatDetailsUrl: String
  val reportVatErrorUrl: String
  val feedbackFormPartialUrl: String
  val contactFormServiceIdentifier: String
  val staticDateValue: String
  val finalReturnPeriodKey: String
  def surveyUrl(identifier: String): String
  def signOutUrl(identifier: String): String
  val unauthorisedSignOutUrl: String
  val vatPaymentsUrl: String
  val selfHost: String
  val timeoutPeriod: Int
  val timeoutCountdown: Int
  val govUkCommercialSoftwareUrl: String
  val languageMap: Map[String, Lang]
  val routeToSwitchLanguage :String => Call
  val submitVatReturnBase: String
  val submitVatReturnUrl: String
  val submitVatReturnForm: String => String
  val submitVatHonestyDeclaration: String => String
  def feedbackUrl(redirect: String): String
  val agentClientLookupUrl: String => String
  val agentClientUnauthorisedUrl: String => String
  val agentClientHubUrl: String
  val gtmContainer: String
}

@Singleton
class FrontendAppConfig @Inject()(implicit configuration: Configuration, sc: ServicesConfig) extends AppConfig {

  override val appName: String = sc.getString("appName")
  private lazy val contactHost: String = sc.getString(Keys.contactFrontendHost)
  override lazy val contactFormServiceIdentifier: String = sc.getString(Keys.contactFrontendService)
  private lazy val contactFrontendService = sc.baseUrl(Keys.contactFrontendService)
  override lazy val reportAProblemPartialUrl: String =
    s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  override lazy val reportAProblemNonJSUrl: String =
    s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"
  override lazy val feedbackFormPartialUrl: String = s"$contactFrontendService/contact/beta-feedback/form"

  private lazy val signInBaseUrl: String = sc.getString(Keys.signInBaseUrl)
  private lazy val signInContinueUrl: String = SafeRedirectUrl(vatDetailsUrl).encodedUrl
  private lazy val signInOrigin = sc.getString("appName")
  override lazy val signInUrl: String = s"$signInBaseUrl?continue=$signInContinueUrl&origin=$signInOrigin"

  override val features = new Features(configuration)

  override val portalUrl: String => String = (vrn: String) => s"/vat-file/trader/$vrn/periods"
  override val vatApiBaseUrl: String = sc.baseUrl("vat-api")
  override val vatReturnsBaseUrl: String = sc.baseUrl("vat-returns")
  override lazy val vatObligationsBaseUrl: String = sc.baseUrl(Keys.vatObligations)
  override val vatSubscriptionBaseUrl: String = sc.baseUrl("vat-subscription")
  override val financialDataBaseUrl: String = sc.baseUrl("financial-transactions")

  override lazy val btaBaseUrl: String = sc.baseUrl(Keys.businessTaxAccount)
  override lazy val btaHomeUrl: String = sc.getString(Keys.businessTaxAccountHost) + sc.getString(Keys.businessTaxAccountUrl)

  private lazy val vatSummaryBase: String = sc.getString(Keys.vatSummaryBase)
  override lazy val vatDetailsUrl: String = vatSummaryBase + sc.getString(Keys.vatDetailsUrl)
  override lazy val vatPaymentsUrl: String = vatSummaryBase + sc.getString(Keys.vatPaymentsUrl)

  override lazy val submitVatReturnBase: String = sc.getString(Keys.submitVatReturnBase)
  override lazy val submitVatReturnUrl: String = submitVatReturnBase + sc.getString(Keys.submitVatReturnUrl)
  override lazy val submitVatReturnForm: String => String = periodKey =>
    submitVatReturnUrl +
    s"/${URLEncoder.encode(periodKey, "utf-8")}" +
    sc.getString(Keys.submitVatReturnForm)
  override lazy val submitVatHonestyDeclaration: String => String = periodKey =>
    submitVatReturnUrl +
      s"/${URLEncoder.encode(periodKey, "utf-8")}" +
      sc.getString(Keys.submitVatHonestyDeclaration)

  override lazy val reportVatErrorUrl: String = sc.getString(Keys.reportVatErrorUrl)

  override lazy val staticDateValue: String = sc.getString(Keys.staticDateValue)

  override lazy val finalReturnPeriodKey: String = sc.getString(Keys.finalReturnPeriodKey)

  private lazy val surveyBaseUrl: String = sc.getString(Keys.surveyHost) + sc.getString(Keys.surveyUrl)
  override def surveyUrl(identifier: String): String = s"$surveyBaseUrl/$identifier"

  private lazy val governmentGatewayHost: String = sc.getString(Keys.governmentGatewayHost)

  override def signOutUrl(identifier: String): String
  = s"$governmentGatewayHost/bas-gateway/sign-out-without-state?continue=${surveyUrl(identifier)}"
  override lazy val unauthorisedSignOutUrl: String
  = s"$governmentGatewayHost/bas-gateway/sign-out-without-state?continue=$signInContinueUrl"

  override lazy val selfHost: String = sc.getString(Keys.selfHost)

  override lazy val timeoutPeriod: Int = sc.getString(Keys.timeoutPeriod).toInt
  override lazy val timeoutCountdown: Int = sc.getString(Keys.timeoutCountDown).toInt

  override lazy val govUkCommercialSoftwareUrl: String = sc.getString(Keys.govUkCommercialSoftwareUrl)

  override val languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  override val routeToSwitchLanguage: String => Call =
    (lang: String) => controllers.routes.LanguageController.switchLanguage(lang)

  override val gtmContainer: String = sc.getString(Keys.gtmContainer)

  private val host: String = sc.getString(Keys.host)

  override def feedbackUrl(redirect: String): String = s"$contactHost/contact/beta-feedback" +
    s"?service=$contactFormServiceIdentifier&backUrl=${SafeRedirectUrl(host + redirect).encodedUrl}"

  private lazy val vatAgentClientLookupFrontendUrl: String =
    sc.getString(Keys.vatAgentClientLookupFrontendHost) + sc.getString(Keys.vatAgentClientLookupFrontendUrl)

  override lazy val agentClientLookupUrl: String => String = uri =>
    vatAgentClientLookupFrontendUrl + s"?redirectUrl=${SafeRedirectUrl(sc.getString(Keys.host) + uri).encodedUrl}"

  override lazy val agentClientUnauthorisedUrl: String => String  = uri =>
    sc.getString(Keys.vatAgentClientLookupFrontendHost) + sc.getString(Keys.vatAgentClientLookupUnauthorisedUrl) +
      s"?redirectUrl=${SafeRedirectUrl(sc.getString(Keys.host) + uri).encodedUrl}"

  override lazy val agentClientHubUrl: String =
    sc.getString(Keys.vatAgentClientLookupFrontendHost) + sc.getString(Keys.vatAgentClientLookupHubUrl)
}
