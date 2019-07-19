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

package controllers

import java.time.LocalDate

import audit.AuditingService
import audit.models.ExtendedAuditModel
import common.SessionKeys
import controllers.predicate.AuthoriseAgentWithClient
import models.{User, _}
import models.errors.ObligationError
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.mvc.{Request, Result}
import play.api.test.Helpers.{charset, contentType, _}
import play.twirl.api.Html
import services.{DateService, EnrolmentsAuthService, ReturnsService, ServiceInfoService}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{User => _, _}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ReturnDeadlinesControllerSpec extends ControllerBaseSpec {

  val agentEnrolment = Enrolments(
    Set(
      Enrolment(
        "HMRC-AS-AGENT",
        Seq(EnrolmentIdentifier("AgentReferenceNumber", "XAIT0000000000")),
        "Activated",
        Some("mtd-vat-auth")
      )
    )
  )

  val goodAgentEnrolments: Future[~[Enrolments, Option[AffinityGroup]]] = Future.successful(new ~(
    agentEnrolment,
    Some(Agent)
  ))

  val goodEnrolments: Future[~[Enrolments, Option[AffinityGroup]]] = Future.successful(new ~(
    Enrolments(
      Set(
        Enrolment(
          "HMRC-MTD-VAT",
          Seq(EnrolmentIdentifier("VRN", vrn)),
          "Active"))
    ),
    Some(Individual)
  ))

  private trait ReturnDeadlinesTest {

    val exampleObligations: Future[ServiceResponse[VatReturnObligations]] = Right(
      VatReturnObligations(
        Seq(
          VatReturnObligation(
            LocalDate.parse("2017-01-01"),
            LocalDate.parse("2017-12-31"),
            LocalDate.parse("2018-01-31"),
            "O",
            None,
            "#001"
          )
        )
      )
    )

    val openObsServiceCall = true
    val serviceInfoCall = true
    val openObligations: Boolean = true
    val authResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.successful(goodEnrolments)
    val agentAuthResult: Future[~[Enrolments, Option[AffinityGroup]]] = Future.successful(goodAgentEnrolments)
    val mockAuthConnector: AuthConnector = mock[AuthConnector]
    val mockVatReturnService: ReturnsService = mock[ReturnsService]
    val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
    val mandationStatusCall: Boolean = false
    val mandationStatusCallResponse: String = "MTDfB"
    val mockDateService: DateService = mock[DateService]
    val mockAuditService: AuditingService = mock[AuditingService]
    val previousYear: Int = 201
    val auditingExpected: Boolean = false
    val mockEnrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)
    val mockAuthorisedAgentWithClient: AuthoriseAgentWithClient = new AuthoriseAgentWithClient(
      mockEnrolmentsAuthService,
      mockVatReturnService,
      messages,
      mockConfig
    )

    val mockAuthorisedController: AuthorisedController = new AuthorisedController(
      mockEnrolmentsAuthService,
      messages,
      mockAuthorisedAgentWithClient,
      mockConfig
    )

    def setup(): Any = {

      (mockDateService.now: () => LocalDate).stubs().returns(LocalDate.parse("2018-05-01")).anyNumberOfTimes()

      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(*, *, *, *)
        .returns(authResult)
        .noMoreThanOnce()

      if (mandationStatusCall) {
        (mockVatReturnService.getMandationStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
          .expects("999999999", *, *)
          .returns(Future.successful(Right(MandationStatus(mandationStatusCallResponse))))
      }

      if (openObsServiceCall) {
        (mockVatReturnService.getOpenReturnObligations(_: User)
        (_: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *)
          .returns(exampleObligations)

        (mockAuditService.extendedAudit(_: ExtendedAuditModel, _: String)(_: HeaderCarrier, _: ExecutionContext))
          .stubs(*, *, *, *)
          .returns({})

        if (authResult != agentAuthResult && serviceInfoCall) {
          (mockServiceInfoService.getServiceInfoPartial(_: Request[_], _: ExecutionContext))
            .expects(*, *)
            .returns(Future.successful(Html("")))
        }
      }

      if (!openObligations) {
        (mockVatReturnService.getFulfilledObligations(_: LocalDate)
        (_: User, _: HeaderCarrier, _: ExecutionContext))
          .expects(*, *, *, *)
          .returns(Right(VatReturnObligations(Seq.empty)))
      }
    }

    def target: ReturnDeadlinesController = {
      setup()
      new ReturnDeadlinesController(
        messages,
        mockEnrolmentsAuthService,
        mockVatReturnService,
        mockAuthorisedController,
        mockDateService,
        mockServiceInfoService,
        mockConfig,
        mockAuditService)
    }

    def agentTarget: ReturnDeadlinesController = {
      setup()
      new ReturnDeadlinesController(
        messages,
        mockEnrolmentsAuthService,
        mockVatReturnService,
        mockAuthorisedController,
        mockDateService,
        mockServiceInfoService,
        mockConfig,
        mockAuditService)
    }
  }

  "Calling the .returnDeadlines action" when {

    "A user is logged in and enrolled to HMRC-MTD-VAT" should {

      "return 200" in new ReturnDeadlinesTest {
        private val result = target.returnDeadlines()(fakeRequest)
        status(result) shouldBe Status.OK
      }

      "return HTML" in new ReturnDeadlinesTest {
        private val result = target.returnDeadlines()(fakeRequest)
        contentType(result) shouldBe Some("text/html")
      }

      "return charset of utf-8" in new ReturnDeadlinesTest {
        private val result = target.returnDeadlines()(fakeRequest)
        charset(result) shouldBe Some("utf-8")
      }
    }

    "for a non-MTDfB user (with the submit return feature enabled)" when {

      "mandation status is in session" should {

        "return the opt-out return deadlines page" in new ReturnDeadlinesTest {
          mockConfig.features.submitReturnFeatures(true)
          override val mandationStatusCall: Boolean = false
          private val result = target.returnDeadlines()(fakeRequest.withSession("mtdVatMandationStatus" -> "Non MTDfB"))
          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(bodyOf(result))
          document.getElementById("submit-return-link").text() shouldBe "Submit VAT Return"
        }
      }

      "mandation status is not in session" should {

        "return the opt-out return deadlines page" in new ReturnDeadlinesTest {
          mockConfig.features.submitReturnFeatures(true)
          override val mandationStatusCall: Boolean = true
          override val mandationStatusCallResponse: String = "Non MTDfB"

          private val result = target.returnDeadlines()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(bodyOf(result))
          document.getElementById("submit-return-link").text() shouldBe "Submit VAT Return"
          result.session.get(SessionKeys.mtdVatMandationStatus) shouldBe Some("Non MTDfB")
        }
      }
    }

    "for an MTDfB user (with the submit return feature enabled)" when {

      "mandation status is in session" should {

        "return the regular return deadlines page" in new ReturnDeadlinesTest {
          mockConfig.features.submitReturnFeatures(true)
          override val mandationStatusCall: Boolean = false

          private val result = target.returnDeadlines()(fakeRequest.withSession("mtdVatMandationStatus" -> "MTDfB Mandated"))

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(bodyOf(result))
          document.getElementById("submit-return-link") shouldBe null
        }
      }

      "mandation status is not in session" should {

        "return the regular return deadlines page" in new ReturnDeadlinesTest {
          mockConfig.features.submitReturnFeatures(true)
          override val mandationStatusCall: Boolean = true
          override val mandationStatusCallResponse: String = "MTDfB Mandated"

          private val result = target.returnDeadlines()(fakeRequest)

          status(result) shouldBe Status.OK
          val document: Document = Jsoup.parse(bodyOf(result))
          document.getElementById("submit-return-link") shouldBe null

          result.session.get(SessionKeys.mtdVatMandationStatus) shouldBe Some("MTDfB Mandated")
        }
      }
    }

    "A user with no open obligations" should {

      "return the no returns view" in new ReturnDeadlinesTest {
        override val exampleObligations: Future[ServiceResponse[VatReturnObligations]] = Right(VatReturnObligations(Seq.empty))
        override val openObligations: Boolean = false

        val result: Result = await(target.returnDeadlines()(fakeRequest))
        val document: Document = Jsoup.parse(bodyOf(result))

        document.select("article > p:nth-child(3)").text() shouldBe
          "You do not have any returns due right now. Your next deadline will show here on the first day of your next" +
            " accounting period."
      }
    }

    "the user is an agent (with agentAccess enabled)" should {

      mockConfig.features.agentAccess(true)

      "return 200, HTML and a charset of utf-8" in new ReturnDeadlinesTest {

        override val authResult: Future[Enrolments ~ Option[AffinityGroup]] = agentAuthResult

        override def setup(): Unit = {
          super.setup()

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, *, *, *)
            .returns(Future.successful(agentEnrolment))
            .noMoreThanOnce()

          (mockVatReturnService.getMandationStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
            .expects(vrn, *, *)
            .returns(Future.successful(Right(MandationStatus("Non MTDfB"))))
        }

        private val result = agentTarget.returnDeadlines()(fakeRequestWithClientsVRN)
        status(result) shouldBe Status.OK
        contentType(result) shouldBe Some("text/html")
        charset(result) shouldBe Some("utf-8")
      }

      "if the client has no open obligations" should {

        "return the no returns view" in new ReturnDeadlinesTest {

          override val authResult: Future[Enrolments ~ Option[AffinityGroup]] = agentAuthResult

          override def setup(): Unit = {
            super.setup()

            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *, *)
              .returns(Future.successful(agentEnrolment))
              .noMoreThanOnce()

            (mockVatReturnService.getMandationStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(vrn, *, *)
              .returns(Future.successful(Right(MandationStatus("Non MTDfB"))))
          }

          override val exampleObligations: Future[ServiceResponse[VatReturnObligations]] = Right(VatReturnObligations(Seq.empty))
          override val openObligations: Boolean = false

          val result: Result = await(agentTarget.returnDeadlines()(fakeRequestWithClientsVRN))
          val document: Document = Jsoup.parse(bodyOf(result))

          document.select("article > p:nth-child(3)").text() shouldBe
            "You do not have any returns due right now. Your next deadline will show here on the first day of your next" +
              " accounting period."
        }
      }

      "if the openObligations call fails" should {

        "return an ISE" in new ReturnDeadlinesTest {

          override val authResult: Future[Enrolments ~ Option[AffinityGroup]] = agentAuthResult

          override def setup(): Unit = {
            super.setup()

            (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *, *)
              .returns(Future.successful(agentEnrolment))
              .noMoreThanOnce()

            (mockVatReturnService.getMandationStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(vrn, *, *)
              .returns(Future.successful(Right(MandationStatus("Non MTDfB"))))

            (mockVatReturnService.getOpenReturnObligations(_: User)
            (_: HeaderCarrier, _: ExecutionContext))
              .expects(*, *, *)
              .returns(exampleObligations)
          }

          override val openObsServiceCall: Boolean = false
          override val exampleObligations: Future[ServiceResponse[Nothing]] = Left(ObligationError)

          val result: Result = await(agentTarget.returnDeadlines()(fakeRequestWithClientsVRN))
          val document: Document = Jsoup.parse(bodyOf(result))

          document.select("h1").first().text() shouldBe "Sorry, there is a problem with the service"
        }
      }
    }

    "A user is not authorised" should {

      "return 403 (Forbidden)" in new ReturnDeadlinesTest {
        override val openObsServiceCall: Boolean = false
        override val serviceInfoCall: Boolean = false
        override val authResult: Future[Nothing] = Future.failed(InsufficientEnrolments())
        private val result = target.returnDeadlines()(fakeRequest)
        status(result) shouldBe Status.FORBIDDEN
      }
    }

    "A user is not authenticated" should {

      "return 401 (Unauthorised)" in new ReturnDeadlinesTest {
        override val openObsServiceCall: Boolean = false
        override val serviceInfoCall: Boolean = false
        override val authResult: Future[Nothing] = Future.failed(MissingBearerToken())
        private val result = target.returnDeadlines()(fakeRequest)
        status(result) shouldBe Status.UNAUTHORIZED
      }
    }

    "the Obligations API call fails" should {

      "return the technical problem view" in new ReturnDeadlinesTest {
        override val serviceInfoCall: Boolean = false
        override val exampleObligations: Future[ServiceResponse[Nothing]] = Left(ObligationError)

        val result: Result = await(target.returnDeadlines()(fakeRequest))
        val document: Document = Jsoup.parse(bodyOf(result))

        document.title shouldBe "There is a problem with the service - VAT reporting through software - GOV.UK"
      }
    }
  }
}
