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

package testOnly.controllers

import javax.inject.Inject

import config.AppConfig
import forms.FeatureSwitchForm
import models.FeatureSwitchModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

class FeatureSwitchController @Inject()(val messagesApi: MessagesApi,
                                        implicit val appConfig: AppConfig)
  extends FrontendController with I18nSupport {

  def featureSwitch: Action[AnyContent] = Action { implicit request =>
    Ok(testOnly.views.html.featureSwitch(FeatureSwitchForm.form.fill(
      FeatureSwitchModel(
        userResearchBannerEnabled = appConfig.features.userResearchBanner(),
        staticDateEnabled = appConfig.features.staticDateEnabled(),
        enableVatReturnsService = appConfig.features.enableVatReturnsService(),
        enableVatObligationsService = appConfig.features.enableVatObligationsService(),
        future2020DateEnabled = appConfig.features.future2020DateEnabled(),
        useLanguageSelector = appConfig.features.useLanguageSelector(),
        submitReturnFeatures = appConfig.features.submitReturnFeatures(),
        agentAccessEnabled = appConfig.features.agentAccess()
      )
    )))
  }

  def submitFeatureSwitch: Action[AnyContent] = Action { implicit request =>
    FeatureSwitchForm.form.bindFromRequest().fold(
      _ => Redirect(routes.FeatureSwitchController.featureSwitch()),
      success = handleSuccess
    )
  }

  def handleSuccess(model: FeatureSwitchModel): Result = {
    appConfig.features.userResearchBanner(model.userResearchBannerEnabled)
    appConfig.features.staticDateEnabled(model.staticDateEnabled)
    appConfig.features.enableVatReturnsService(model.enableVatReturnsService)
    appConfig.features.enableVatObligationsService(model.enableVatObligationsService)
    appConfig.features.future2020DateEnabled(model.future2020DateEnabled)
    appConfig.features.useLanguageSelector(model.useLanguageSelector)
    appConfig.features.submitReturnFeatures(model.submitReturnFeatures)
    appConfig.features.agentAccess(model.agentAccessEnabled)
    Redirect(routes.FeatureSwitchController.featureSwitch())
  }
}
