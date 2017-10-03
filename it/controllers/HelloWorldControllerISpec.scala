
package controllers

import helpers.BaseIntegrationSpec
import play.api.http.Status._
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec

class HelloWorldControllerISpec extends UnitSpec with BaseIntegrationSpec {

  "Calling the helloWorld action" when {

    "authenticated" should {

      "return 200 OK" in {
        given.user.isAuthenticated
        val controller = app.injector.instanceOf[HelloWorldController]
        val result = controller.helloWorld(FakeRequest())
        await(result).header.status shouldBe OK
      }
    }

    "not authenticated" should {

      "return 303 SEE OTHER" in {
        given.user.isNotAuthenticated
        val controller = app.injector.instanceOf[HelloWorldController]
        val result = controller.helloWorld(FakeRequest())
        await(result).header.status shouldBe SEE_OTHER
      }
    }

  }

}
