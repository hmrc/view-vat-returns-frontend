
package stubs

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.Constants.MTD_VAT_ENROLMENT_KEY
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}

object AuthStub extends WireMockMethods {

  private val authoriseUri = "/auth/authorise"

  def stubAuthSuccess(): StubMapping = {
    when(method = POST, uri = authoriseUri)
      .thenReturn(status = OK, body = successfulAuthResponse(mtdVatEnrolment))
  }

  private val mtdVatEnrolment = Json.obj(
    "key" -> MTD_VAT_ENROLMENT_KEY,
    "identifiers" -> Json.arr(
      Json.obj(
        "key" -> "MtdVatId",
        "value" -> "ChazDingle"
      )
    )
  )

  private def successfulAuthResponse(enrolments: JsObject*): JsObject = {
    Json.obj("allEnrolments" -> enrolments)
  }

}
