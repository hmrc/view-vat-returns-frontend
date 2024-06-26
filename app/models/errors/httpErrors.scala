/*
 * Copyright 2024 HM Revenue & Customs
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

package models.errors

sealed trait HttpError {
  def message: String
}

case class BadRequestError(code: String, errorResponse: String) extends HttpError {
  override val message: String = s"The server you connecting to returned an error. " +
    s"[BadRequest]- RESPONSE status: $code, message: $errorResponse"
}

case class ServerSideError(code: String, errorResponse: String) extends HttpError {
  override val message: String = s"The server you connecting to returned an error. " +
    s"[ServerSideError]- RESPONSE status: $code, body: $errorResponse"
}

case class UnexpectedStatusError(code: String, errorResponse: String) extends HttpError {
  override val message: String = s"The server you connecting to returned an unexpected error." +
    s"[UnexpectedStatusError]- RESPONSE status: $code, body: $errorResponse"
}

object UnexpectedJsonFormat extends HttpError {
  override val message: String = "The server you connecting to returned unexpected JSON."
}

case class MultipleErrors(code: String, errorResponse: String) extends HttpError {
  override val message: String = s"The server you connecting to returned an error. " +
    s"[MultipleErrors]- RESPONSE status: $code, body: $errorResponse"
}

case object UnknownError extends HttpError {
  override val message: String = "Received an unknown error."
}
