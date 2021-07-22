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

package connectors

import config.AppConfig
import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.partials.HtmlPartial.{HtmlPartialHttpReads, connectionExceptionsAsHtmlPartialFailure}
import uk.gov.hmrc.play.partials.{HeaderCarrierForPartialsConverter, HtmlPartial}
import utils.LoggerUtil
import views.html.templates.BtaNavigationLinks

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ServiceInfoConnector @Inject()(http: HttpClient,
                                     hcForPartials: HeaderCarrierForPartialsConverter,
                                     btaNavigationLinks: BtaNavigationLinks)
                                    (implicit val messagesApi: MessagesApi,
                                     appConfig: AppConfig)
  extends HtmlPartialHttpReads
    with I18nSupport
    with LoggerUtil {

  lazy val btaUrl: String = appConfig.btaBaseUrl + "/business-account/partial/service-info"

  def getServiceInfoPartial(implicit request: Request[_], ec: ExecutionContext): Future[Html] = {
    implicit val hc: HeaderCarrier = hcForPartials.fromRequestWithEncryptedCookie(request)
    http.GET[HtmlPartial](btaUrl) recover connectionExceptionsAsHtmlPartialFailure map { p =>
      p.successfulContentOrElse(btaNavigationLinks())
    } recover {
      case _ =>
        logger.warn("[ServiceInfoConnector][getServiceInfoPartial] - Unexpected error retrieving service info partial")
        btaNavigationLinks()
    }
  }
}
