
package uk.gov.hmrc.viewvatreturnsfrontend.controllers

import controllers.HelloWorld
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.http._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}


class HelloWorldControllerSpec extends UnitSpec with WithFakeApplication{

  val fakeRequest = FakeRequest("GET", "/")


  "GET /" should {
    "return 200" in {
      val result = HelloWorld.helloWorld(fakeRequest)
      status(result) shouldBe Status.OK
    }

    "return HTML" in {
      val result = HelloWorld.helloWorld(fakeRequest)
      contentType(result) shouldBe Some("text/html")
      charset(result) shouldBe Some("utf-8")
    }


  }


}
