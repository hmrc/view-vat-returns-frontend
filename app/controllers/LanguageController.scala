/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers

import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import uk.gov.hmrc.play.language.LanguageUtils

@Singleton
class LanguageController @Inject()(val appConfig: AppConfig,
                                      val messagesApi: MessagesApi) extends FrontendController with I18nSupport {

  def langToCall: String => Call = appConfig.routeToSwitchLanguage

  protected[controllers] def fallbackURL: String = appConfig.vatDetailsUrl

  def languageMap: Map[String, Lang] = appConfig.languageMap

  def switchLanguage(language: String): Action[AnyContent] = Action { implicit request =>
    val lang = languageMap.getOrElse(language, LanguageUtils.getCurrentLang)
    val redirectUrl = request.headers.get(REFERER).getOrElse(fallbackURL)

    Redirect(redirectUrl).withLang(Lang(lang.code)).flashing(LanguageUtils.FlashWithSwitchIndicator)
  }
}
