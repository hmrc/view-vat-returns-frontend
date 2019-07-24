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

package mocks

import java.time.LocalDate

import audit.AuditingService
import audit.models.{AuditModel, ExtendedAuditModel}
import config.AppConfig
import connectors.{VatObligationsConnector, VatSubscriptionConnector}
import connectors.httpParsers.ResponseHttpParsers.HttpGetResult
import controllers.predicate.AuthoriseAgentWithClient
import controllers.AuthorisedController
import models.Obligation.Status
import models.customer.CustomerDetail
import models.payments.Payment
import models.{Obligation, _}
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.twirl.api.Html
import services._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

trait MockAuth extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach with MockFactory {

  lazy val messages: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val mockConfig: AppConfig = new MockAppConfig(app.configuration)

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockVatReturnService: ReturnsService = mock[ReturnsService]
  val mockDateService: DateService = mock[DateService]
  val mockAuditService: AuditingService = mock[AuditingService]
  val mockServiceInfoService: ServiceInfoService = mock[ServiceInfoService]
  val mockSubscriptionService: SubscriptionService = mock[SubscriptionService]
  val mockVatObligationsConnector: VatObligationsConnector = mock[VatObligationsConnector]
  val mockVatSubscriptionConnector: VatSubscriptionConnector = mock[VatSubscriptionConnector]

  val enrolmentsAuthService: EnrolmentsAuthService = new EnrolmentsAuthService(mockAuthConnector)

  val mockAuthorisedAgentWithClient: AuthoriseAgentWithClient = new AuthoriseAgentWithClient(
    enrolmentsAuthService,
    mockVatReturnService,
    messages,
    mockConfig
  )

  val mockAuthorisedController: AuthorisedController = new AuthorisedController(
    enrolmentsAuthService,
    messages,
    mockAuthorisedAgentWithClient,
    mockConfig
  )

  def callDateService(response: LocalDate = LocalDate.parse("2018-05-01")): Any =
    (mockDateService.now: () => LocalDate)
      .stubs()
      .returns(response)

  def callMandationService(response: ServiceResponse[MandationStatus]): Any =
    (mockVatReturnService.getMandationStatus(_: String)(_: HeaderCarrier, _: ExecutionContext))
    .expects(*, *, *)
    .returns(Future.successful(response))

  def callOpenObligationsForYear(response: ServiceResponse[VatReturnObligations]): Any =
    (mockVatReturnService.getReturnObligationsForYear(_: User, _: Int, _: Obligation.Status.Value)
    (_: HeaderCarrier, _: ExecutionContext, _: Option[String]))
      .expects(*, *, *, *, *, *)
      .returns(Future.successful(response))

  def callOpenObligations(response: ServiceResponse[VatReturnObligations]): Any =
    (mockVatReturnService.getOpenReturnObligations(_: User)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
      .returns(Future.successful(response))

  def callFulfilledObligations(response: ServiceResponse[VatReturnObligations]): Any =
    (mockVatReturnService.getFulfilledObligations(_: LocalDate)(_: User, _: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returns(Future.successful(response))

  def callVatReturn(response: ServiceResponse[VatReturn]): Any =
    (mockVatReturnService.getVatReturn(_: User, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returns(Future.successful(response))

  def callObligationWithMatchingPeriodKey(response: Option[VatReturnObligation]): Any =
    (mockVatReturnService.getObligationWithMatchingPeriodKey(_: User, _: Int, _: String)(_: HeaderCarrier, _: ExecutionContext, _: Option[String]))
      .expects(*, *, *, *, *, *)
      .returns(Future.successful(response))

  def callVatReturnPayment(response: Option[Payment]): Any =
    (mockVatReturnService.getPayment(_: User, _: String, _: Option[Int])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *, *)
      .returns(response)

  def callExtendedAudit: Any =
    (mockAuditService.extendedAudit(_: ExtendedAuditModel, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .stubs(*, *, *, *)
      .returns({})

  def callAudit: Any =
    (mockAuditService.audit(_: AuditModel, _: String)(_: HeaderCarrier, _: ExecutionContext))
      .stubs(*, *, *, *)
      .returns({})

  def callServiceInfoPartialService: Any =
    (mockServiceInfoService.getServiceInfoPartial(_: Request[_], _: ExecutionContext))
      .expects(*, *)
      .returns(Future.successful(Html("")))

  def callAuthService(response: Future[~[Enrolments, Option[AffinityGroup]]]): Any =
    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[~[Enrolments, Option[AffinityGroup]]])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returns(response)

  def callAuthServiceEnrolmentsOnly(response: Enrolments): Any =
    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[Enrolments])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returns(Future.successful(response))

  def callSubscriptionService(response: Option[CustomerDetail]): Any =
    (mockSubscriptionService.getUserDetails(_: User)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
      .returns(Future.successful(response))

  def callSubscriptionConnector(response: HttpGetResult[CustomerInformation]): Any =
    (mockVatSubscriptionConnector.getCustomerInfo(_: String)(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *)
      .returns(Future.successful(response))

  def callConstructReturnDetailsModel(response: VatReturnDetails): Any =
    (mockVatReturnService.constructReturnDetailsModel(_: VatReturn, _: Option[Payment]))
      .expects(*, *)
      .returns(response)

  def callObligationsConnector(response: HttpGetResult[VatReturnObligations]): Any =
    (mockVatObligationsConnector.getVatReturnObligations(_: String, _: Option[LocalDate], _: Option[LocalDate], _: Status.Value)
    (_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *, *, *)
      .returns(Future.successful(response))
}
