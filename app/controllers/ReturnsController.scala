/*
 * Copyright 2022 HM Revenue & Customs
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

import audit.AuditingService
import audit.models.ViewVatReturnAuditModel
import common.SessionKeys
import config.{AppConfig, ServiceErrorHandler}
import javax.inject.{Inject, Singleton}
import models._
import models.errors.NotFoundError
import models.payments.Payment
import models.viewModels.VatReturnViewModel
import play.api.mvc._
import play.twirl.api.HtmlFormat
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.LoggerUtil
import views.html.errors.PreMtdReturnView
import views.html.returns.VatReturnDetailsView
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnsController @Inject()(mcc: MessagesControllerComponents,
                                  returnsService: ReturnsService,
                                  subscriptionService: SubscriptionService,
                                  dateService: DateService,
                                  serviceInfoService: ServiceInfoService,
                                  authorisedController: AuthorisedController,
                                  vatReturnDetailsView: VatReturnDetailsView,
                                  errorHandler: ServiceErrorHandler,
                                  preMtdReturnView: PreMtdReturnView)
                                 (implicit val appConfig: AppConfig,
                                  auditService: AuditingService,
                                  ec: ExecutionContext) extends FrontendController(mcc) with LoggerUtil {

  def vatReturn(year: Int, periodKey: String): Action[AnyContent] = authorisedController.authorisedAction {
    implicit request =>
      implicit user =>
        if(validPeriodKey(periodKey)) {
          val isReturnsPageRequest = true
          val vatReturnCall = returnsService.getVatReturn(user.vrn, periodKey)
          val entityNameCall = subscriptionService.getUserDetails(user.vrn)
          val obligationCall = returnsService.getObligationWithMatchingPeriodKey(user.vrn, year, periodKey)

          def financialDataCall(customerInfo: Option[CustomerInformation]): Future[Option[Payment]] = {
            val isHybridUser = customerInfo.exists(_.isPartialMigration)
            if (isHybridUser) Future.successful(None) else returnsService.getPayment(user.vrn, periodKey, Some(year))
          }

          (for {
            vatReturn <- vatReturnCall
            customerInfo <- entityNameCall
            payment <- financialDataCall(customerInfo)
            obligation <- obligationCall
            serviceInfoContent <- serviceInfoService.getServiceInfoPartial
          } yield ReturnsControllerData(vatReturn, customerInfo, payment, obligation, serviceInfoContent))
            .flatMap(pageData => renderResult(pageData, isReturnsPageRequest, numericPeriodKey(periodKey)))

        } else {
          logger.warn(s"[ReturnsController][vatReturn] - The given period key was invalid - `$periodKey`")
          Future.successful(errorHandler.showNotFoundError)
        }
  }

  def vatReturnViaPayments(periodKey: String): Action[AnyContent] = authorisedController.authorisedAction {
    implicit request =>
      implicit user =>
        if(validPeriodKey(periodKey)) {
          val isReturnsPageRequest = false
          val vatReturnCall = returnsService.getVatReturn(user.vrn, periodKey)
          val entityNameCall = subscriptionService.getUserDetails(user.vrn)
          val financialDataCall = returnsService.getPayment(user.vrn, periodKey)

          def obligationCall(payment: Option[Payment]) = {
            payment.fold(Future.successful(Option.empty[VatReturnObligation])) { p =>
              returnsService.getObligationWithMatchingPeriodKey(user.vrn, p.periodTo.getYear, periodKey)
            }
          }

          (for {
            vatReturnResult <- vatReturnCall
            customerInfo <- entityNameCall
            payment <- financialDataCall
            obligation <- obligationCall(payment)
            serviceInfoContent <- if(user.isAgent) Future.successful(HtmlFormat.empty) else serviceInfoService.getServiceInfoPartial
          } yield ReturnsControllerData(vatReturnResult, customerInfo, payment, obligation, serviceInfoContent))
            .flatMap(pageData => renderResult(pageData, isReturnsPageRequest, numericPeriodKey(periodKey)))

        } else {
          logger.warn(s"[ReturnsController][vatReturnViaPayments] - The given period key was invalid - `$periodKey`")
          Future.successful(errorHandler.showNotFoundError)
        }
  }

  private[controllers] def renderResult(pageData: ReturnsControllerData,
                                        isReturnsPageRequest: Boolean, isNumericPeriodKey: Boolean)
                                       (implicit req: MessagesRequest[AnyContent], user: User): Future[Result] = {
    (pageData.vatReturnResult, pageData.obligation, pageData.payment) match {
      case (Right(vatReturn), Some(ob), payment) =>
        val returnDetails = returnsService.constructReturnDetailsModel(vatReturn, payment)
        def viewModel = constructViewModel(
          pageData.customerInfo,
          ob,
          returnDetails,
          isReturnsPageRequest
        )
        val model = viewModel
        auditEvent(isReturnsPageRequest, model)
        Future.successful(Ok(vatReturnDetailsView(model, pageData.serviceInfoContent)))
      case (Left(NotFoundError), _, _) =>
        checkIfComingFromSubmissionConfirmation(isNumericPeriodKey)
      case (Right(_), None, _) =>
        logger.warn("[ReturnsController][renderResult] error: render required a valid obligation but none was returned")
        Future.successful(errorHandler.showInternalServerError)
      case _ =>
        logger.warn("[ReturnsController][renderResult] error: Unknown error")
        Future.successful(errorHandler.showInternalServerError)
    }
  }

  private def checkIfComingFromSubmissionConfirmation(preMtdReturn: Boolean)
                                                     (implicit req: MessagesRequest[AnyContent],
                                                      user: User): Future[Result] = {
    val inSessionSubmissionYear = req.session.get(SessionKeys.submissionYear)
    val inSessionPeriodKey = req.session.get(SessionKeys.inSessionPeriodKey)

    if(inSessionSubmissionYear.nonEmpty && inSessionPeriodKey.nonEmpty) {
      logger.warn(
        "[ReturnsController][checkIfComingFromSubmissionConfirmation] error: User has come from the submission confirmation page, " +
        "but their submission has not yet been processed."
      )
      Future.successful(
        Redirect(routes.SubmittedReturnsController.submittedReturns).removingFromSession(SessionKeys.submissionYear, SessionKeys.inSessionPeriodKey)
      )
    } else {
      if(preMtdReturn) {
        Future.successful(NotFound(preMtdReturnView(user)))
      } else {
        Future.successful(errorHandler.showNotFoundError)
      }
    }
  }

  private def auditEvent(isReturnsPageRequest: Boolean, data: VatReturnViewModel)(implicit user: User, hc: HeaderCarrier): Unit = {

    val periodKey = data.vatReturnDetails.vatReturn.periodKey

    val auditPath = if (isReturnsPageRequest) {
      routes.ReturnsController.vatReturn(data.periodFrom.getYear, periodKey).url
    }
    else {
      routes.ReturnsController.vatReturnViaPayments(periodKey).url
    }

    auditService.audit(ViewVatReturnAuditModel(user, data), auditPath)
  }

  private[controllers] def constructViewModel(customerDetail: Option[CustomerInformation],
                                              obligation: VatReturnObligation,
                                              returnDetails: VatReturnDetails,
                                              isReturnsPageRequest: Boolean): VatReturnViewModel = {

    val amountToShow: BigDecimal = returnDetails.vatReturn.netVatDue

    VatReturnViewModel(
      entityName = customerDetail.fold[Option[String]](None)(detail => detail.entityName),
      periodFrom = obligation.periodFrom,
      periodTo = obligation.periodTo,
      dueDate = obligation.due,
      returnTotal = amountToShow,
      dateSubmitted = obligation.received.get,
      vatReturnDetails = returnDetails,
      showReturnsBreadcrumb = isReturnsPageRequest,
      currentYear = dateService.now().getYear,
      hasFlatRateScheme = customerDetail.fold(false)(_.hasFlatRateScheme),
      isHybridUser = customerDetail.fold(false)(_.isPartialMigration)
    )
  }

  private[controllers] def validPeriodKey(periodKey: String): Boolean = {
    val periodKeyRegex = """^([0-9 A-Z]{4})$|^(#[0-9]{3})$"""
    periodKey.matches(periodKeyRegex)
  }

  private[controllers] def numericPeriodKey(periodKey: String): Boolean = {
    val numericPeriodKeyRegex = """^([0-9]{4})$"""
    periodKey.matches(numericPeriodKeyRegex)
  }
}
