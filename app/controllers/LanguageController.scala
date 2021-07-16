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

package controllers

import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.i18n.{Lang, Langs}
import play.api.mvc.{Action, AnyContent, Call, Flash, MessagesControllerComponents, RequestHeader}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

@Singleton
class LanguageController @Inject()(langs: Langs,
                                   appConfig: AppConfig,
                                   mcc: MessagesControllerComponents) extends FrontendController(mcc) {

  def langToCall: String => Call = appConfig.routeToSwitchLanguage

  protected[controllers] def fallbackURL: String = appConfig.vatDetailsUrl

  def languageMap: Map[String, Lang] = appConfig.languageMap

  def getCurrentLang(implicit request: RequestHeader): Lang = {
    val maybeLangFromCookie = request.cookies.get(mcc.messagesApi.langCookieName).flatMap(c => Lang.get(c.value))
    maybeLangFromCookie.getOrElse(langs.preferred(request.acceptLanguages))
  }

  def switchLanguage(language: String): Action[AnyContent] = Action { implicit request =>
    val lang = languageMap.getOrElse(language, getCurrentLang)
    val redirectUrl = request.headers.get(REFERER).getOrElse(fallbackURL)

    mcc.messagesApi.setLang(Redirect(redirectUrl).flashing(Flash(Map("switching-language" -> "true"))), lang)
  }
}
