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

package connectors.httpParsers

import connectors.httpParsers.ResponseHttpParsers.HttpGetResult
import models.errors.{ApiSingleError, ServerSideError, UnexpectedJsonFormat, UnexpectedStatusError}
import models.payments.Payments
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK}
import play.api.libs.json.{JsArray, JsValue, Json}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.LoggerUtil

import scala.util.{Failure, Success, Try}

object PaymentsHttpParser extends ResponseHttpParsers with LoggerUtil {

  implicit object PaymentsReads extends HttpReads[HttpGetResult[Payments]] {
    override def read(method: String, url: String, response: HttpResponse): HttpGetResult[Payments] = {
      response.status match {
        case OK => Try(removeNonVatReturnCharges(response.json).as[Payments]) match {
          case Success(model) => Right(model)
          case Failure(_) =>
            logger.debug(s"[PaymentsReads][read] Could not parse JSON. Received: ${response.json}")
            logger.warn("[PaymentsReads][read] Unexpected JSON received.")
            Left(UnexpectedJsonFormat)
        }
        case NOT_FOUND => Right(Payments(Seq.empty))
        case BAD_REQUEST => handleBadRequest(response.json)(ApiSingleError.desSingleErrorReads)
        case status if status >= 500 && status < 600 => Left(ServerSideError(response.status.toString, response.body))
        case _ => Left(UnexpectedStatusError(response.status.toString, response.body))
      }
    }
  }

  private def removeNonVatReturnCharges(json: JsValue): JsValue = {

    val validCharges: Set[String] = Set(
      "VAT Return Debit Charge",
      "VAT Return Credit Charge",
      "VAT EC Debit Charge",
      "VAT AA Return Debit Charge",
      "VAT AA Return Credit Charge",
      "VAT POA Return Debit Charge",
      "VAT POA Return Credit Charge"
    )

    val charges: Seq[JsValue] = (json \ "financialTransactions").as[JsArray].value

    val vatReturnCharges = charges.filter { charge =>
      val chargeType: String = (charge \ "chargeType").as[String]
      validCharges.contains(chargeType)
    }

    Json.obj("financialTransactions" -> JsArray(vatReturnCharges))
  }
}
