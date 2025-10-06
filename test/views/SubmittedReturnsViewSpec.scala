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

package views

import java.time.LocalDate

import models.viewModels.{ReturnObligationsViewModel, VatReturnsViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.exceptions.TestFailedException
import play.twirl.api.Html
import views.html.returns.SubmittedReturnsView

class SubmittedReturnsViewSpec extends ViewBaseSpec {

  val injectedView: SubmittedReturnsView = inject[SubmittedReturnsView]

  object Selectors {
    val pageHeading = "h1"
    val secondaryHeading = "h2"
    val btaBreadcrumb = ".govuk-breadcrumbs li:nth-of-type(1)"
    val btaBreadcrumbLink = s"$btaBreadcrumb a"
    val vatBreadcrumb = ".govuk-breadcrumbs li:nth-of-type(2)"
    val vatBreadcrumbLink = s"$vatBreadcrumb a"
    val submitThroughSoftwareLink = "div > div > a"
    val noReturnsFound = ".govuk-tabs__panel > p"
    val tabOne = "#content > div.govuk-tabs > ul > li:nth-child(1) > a"
    val tabTwo = "#content > div.govuk-tabs > ul > li:nth-child(2) > a"
    val tabThree = "#content > div.govuk-tabs > ul > li:nth-child(3) > a"
    val tabFour = "#content > div.govuk-tabs > ul > li:nth-child(4) > a"
    val prevReturnsHeading = "#previous-returns > h1"
    val prevReturnsParagraph = "#previous-returns > p"
    val returnsHeading = "div.govuk-tabs__panel > h2.govuk-heading-m"
    val period = "div.govuk-tabs__panel > p.govuk-body"
    val backLink = ".govuk-back-link"
    val insolvencyContent = "#insolvency-notice"
    val recentlySubmittedReturnNotice = "#return-recently-submitted-content"

    def obligation(number: Int): String = s".govuk-list--bullet li:nth-of-type($number)"

    def obligationLink(number: Int): String = s".govuk-list--bullet li:nth-of-type($number) > a"

    val previousReturnsText = "#previous-one"
    val previousReturnsLink = "#previous-one a:nth-of-type(1)"
  }

  lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
    Seq(
      ReturnObligationsViewModel(
        LocalDate.parse("2018-01-01"),
        LocalDate.parse("2018-03-31"),
        "#001"
      ),
      ReturnObligationsViewModel(
        LocalDate.parse("2018-04-01"),
        LocalDate.parse("2018-06-30"),
        "#002"
      )
    )

  "Rendering the Submitted Returns page with a HMRC-MTD-VAT enrolment" when {

    "the user is not insolvent" when {

      "no submitted return is in session" when {

        "there is a single return year retrieved" when {

          "there are multiple returns for the year retrieved" should {


            lazy val view: Html = injectedView(
              VatReturnsViewModel(
                returnYears = Seq(2018),
                obligations = exampleReturns,
                showPreviousReturnsTab = false,
                vrn = vrn
              ),
              showInsolvencyContent = false,
              recentlySubmittedReturn = false
            )

            lazy implicit val document: Document = Jsoup.parse(view.body)

            "have the correct document title" in {
              document.title shouldBe "Submitted returns - Manage your VAT account - GOV.UK"
            }

            "have the correct page heading" in {
              elementText(Selectors.pageHeading) shouldBe "Submitted returns"
            }

            "render breadcrumbs which" should {

              "have the text 'Business tax account'" in {
                elementText(Selectors.btaBreadcrumb) shouldBe "Business tax account"
              }

              "link to BTA" in {
                element(Selectors.btaBreadcrumbLink).attr("href") shouldBe "bta-url"
              }

              "have the text 'VAT'" in {
                elementText(Selectors.vatBreadcrumb) shouldBe "Your VAT account"
              }

              "link to the VAT Details page" in {
                element(Selectors.vatBreadcrumbLink).attr("href") shouldBe "vat-details-url"
              }
            }

            "not render back button" in {
              an[TestFailedException] should be thrownBy element(Selectors.backLink)
            }

            "not display the insolvency content" in {
              elementExtinct(Selectors.insolvencyContent)
            }

            "have the correct return heading" in {
              elementText(Selectors.returnsHeading) shouldBe "2018 returns"
            }

            "have the correct period text" in {
              elementText(Selectors.period) shouldBe "For the period:"
            }

            "contain the first return which" should {

              "contains the correct obligation period text" in {
                elementText(Selectors.obligation(1)) shouldBe "View return for the period 1 January to 31 March 2018"
              }

              "contains the correct obligation period text using non breaking space" in {
                elementText(Selectors.obligation(1)).contains("View return for the period 1\u00a0January to 31\u00a0March\u00a02018")
              }

              "contains the correct link to view a specific return" in {
                element(Selectors.obligationLink(1)).attr("href") shouldBe controllers.routes.ReturnsController.vatReturn(2018, "#001").url
              }
            }

            "contain the second return which" should {

              "contains the correct obligation period text" in {
                elementText(Selectors.obligation(2)) shouldBe "View return for the period 1 April to 30 June 2018"
              }

              "contains the correct obligation period text using non breaking space" in {
                elementText(Selectors.obligation(2)).contains("View return for the period 1\u00a0April to 30\u00a0June\u00a02018")
              }

              "contains the correct link to view a specific return" in {
                element(Selectors.obligationLink(2)).attr("href") shouldBe controllers.routes.ReturnsController.vatReturn(2018, "#002").url
              }
            }

            "have a single tab" in {
              elementText(Selectors.tabOne) should include("2018")
              elementExtinct(Selectors.tabTwo)
            }

            "not display the notice about recently submitted returns" in {
              elementExtinct(Selectors.recentlySubmittedReturnNotice)
            }
          }

          "there is a final return for year of 2018" should {

            val exampleReturn: Seq[ReturnObligationsViewModel] =
              Seq(
                ReturnObligationsViewModel(
                  LocalDate.parse("2018-01-01"),
                  LocalDate.parse("2018-12-31"),
                  mockConfig.finalReturnPeriodKey
                )
              )

            lazy val view: Html = injectedView(
              VatReturnsViewModel(
                returnYears = Seq(2018),
                obligations = exampleReturn,
                showPreviousReturnsTab = false,
                vrn = vrn
              ),
              showInsolvencyContent = false,
              recentlySubmittedReturn = false
            )

            lazy implicit val document: Document = Jsoup.parse(view.body)

            "contain the first obligation" in {
              elementText(Selectors.obligation(1)) shouldBe "View return for the period Final return"
            }

            "not display the notice about recently submitted returns" in {
              elementExtinct(Selectors.recentlySubmittedReturnNotice)
            }
          }
        }

        "two return years are retrieved" should {

          val returnYears = Seq(2020, 2019)

          lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
            Seq(
              ReturnObligationsViewModel(
                LocalDate.parse("2020-01-01"),
                LocalDate.parse("2020-03-31"),
                "#001"
              ),
              ReturnObligationsViewModel(
                LocalDate.parse("2020-04-01"),
                LocalDate.parse("2020-06-30"),
                "#002"
              ),
              ReturnObligationsViewModel(
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2019-03-31"),
                "#001"
              ),
              ReturnObligationsViewModel(
                LocalDate.parse("2019-04-01"),
                LocalDate.parse("2019-06-30"),
                "#002"
              )
            )

          lazy val view = injectedView(
            VatReturnsViewModel(returnYears, exampleReturns, showPreviousReturnsTab = false, vrn),
            showInsolvencyContent = false,
            recentlySubmittedReturn = false
          )

          lazy implicit val document: Document = Jsoup.parse(view.body)

          "have a tab for 2020" in {
            elementText(Selectors.tabOne) should include("2020")
          }

          "have a tab for 2019" in {
            elementText(Selectors.tabTwo) should include("2019")
          }

          "not display the previous returns tab" in {
            elementAsOpt(Selectors.tabThree) shouldBe None
          }

          "not display the notice about recently submitted returns" in {
            elementExtinct(Selectors.recentlySubmittedReturnNotice)
          }
        }

        "more than 2 return years are retrieved" should {

          val returnYears = Seq(2020, 2019, 2018)

          lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
            Seq(
              ReturnObligationsViewModel(
                LocalDate.parse("2020-01-01"),
                LocalDate.parse("2020-03-31"),
                "#001"
              ),
              ReturnObligationsViewModel(
                LocalDate.parse("2020-04-01"),
                LocalDate.parse("2020-06-30"),
                "#002"
              ),
              ReturnObligationsViewModel(
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2019-03-31"),
                "#001"
              ),
              ReturnObligationsViewModel(
                LocalDate.parse("2019-04-01"),
                LocalDate.parse("2019-06-30"),
                "#002"
              ),
              ReturnObligationsViewModel(
                LocalDate.parse("2018-01-01"),
                LocalDate.parse("2018-03-31"),
                "#001"
              ),
              ReturnObligationsViewModel(
                LocalDate.parse("2018-04-01"),
                LocalDate.parse("2018-06-30"),
                "#002"
              )
            )

          lazy val view = injectedView(
            VatReturnsViewModel(returnYears, exampleReturns, showPreviousReturnsTab = false, vrn),
            showInsolvencyContent = false,
            recentlySubmittedReturn = false
          )

          lazy implicit val document: Document = Jsoup.parse(view.body)

          "have a tab for 2020" in {
            elementText(Selectors.tabOne) should include("2020")
          }

          "have a tab for 2019" in {
            elementText(Selectors.tabTwo) should include("2019")
          }

          "have a tab for 2018" in {
            elementText(Selectors.tabThree) should include("2018")
          }

          "not display the previous returns tab" in {
            elementAsOpt(Selectors.tabFour) shouldBe None
          }

          "not display the notice about recently submitted returns" in {
            elementExtinct(Selectors.recentlySubmittedReturnNotice)
          }
        }

        "there are no returns for the year retrieved" should {

          lazy val view = injectedView(
            VatReturnsViewModel(Seq(2018), Seq(), showPreviousReturnsTab = false, vrn),
            showInsolvencyContent = false,
            recentlySubmittedReturn = false
          )

          lazy implicit val document: Document = Jsoup.parse(view.body)

          "have the correct return heading" in {
            elementText(Selectors.returnsHeading) shouldBe "2018 returns"
          }

          "have the correct alternate content" in {
            elementText(Selectors.noReturnsFound) shouldBe
              "You have not submitted any returns using the new VAT service this year."
          }

          "not display the notice about recently submitted returns" in {
            elementExtinct(Selectors.recentlySubmittedReturnNotice)
          }
        }
      }

      "there is a submitted return in session" should {

        lazy val view = injectedView(
          VatReturnsViewModel(Seq(2018), Seq(), showPreviousReturnsTab = false, vrn),
          showInsolvencyContent = false,
          recentlySubmittedReturn = true
        )

        lazy implicit val document: Document = Jsoup.parse(view.body)

        "display the notice about recently submitted returns" in {
          elementText(Selectors.recentlySubmittedReturnNotice) shouldBe "Check back later if you cannot see a recently submitted return."
        }
      }
    }

    "the user is insolvent" should {

      lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
        Seq(
          ReturnObligationsViewModel(
            LocalDate.parse("2018-01-01"),
            LocalDate.parse("2018-03-31"),
            "#001"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2018-04-01"),
            LocalDate.parse("2018-06-30"),
            "#002"
          )
        )

      lazy val view: Html = injectedView(
        VatReturnsViewModel(
          returnYears = Seq(2018),
          obligations = exampleReturns,
          showPreviousReturnsTab = false,
          vrn = vrn
        ),
        showInsolvencyContent = true,
        recentlySubmittedReturn = false
      )

      lazy implicit val document: Document = Jsoup.parse(view.body)

      "display the insolvency content" in {
        elementText(Selectors.insolvencyContent) shouldBe "You cannot view returns made before the insolvency date."
      }

      "not display the notice about recently submitted returns" in {
        elementExtinct(Selectors.recentlySubmittedReturnNotice)
      }
    }
  }

  "Rendering the Submitted Returns page with a HMRC-MTD-VAT enrolment assigned to an agent" when {

    "there is no recently submitted return in session" when {

      "there is a single return year retrieved" when {

        "there are multiple returns for the year retrieved" should {

          lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
            Seq(
              ReturnObligationsViewModel(
                LocalDate.parse("2018-01-01"),
                LocalDate.parse("2018-03-31"),
                "#001"
              ),
              ReturnObligationsViewModel(
                LocalDate.parse("2018-04-01"),
                LocalDate.parse("2018-06-30"),
                "#002"
              )
            )

          lazy val view: Html = injectedView(
            VatReturnsViewModel(
              returnYears = Seq(2018),
              obligations = exampleReturns,
              showPreviousReturnsTab = false,
              vrn = vrn
            ),
            showInsolvencyContent = false,
            recentlySubmittedReturn = false
          )(fakeRequestWithClientsVRN, messages, mockConfig, agentUser)

          lazy implicit val document: Document = Jsoup.parse(view.body)

          "have the correct document title" in {
            document.title shouldBe "Submitted returns - Your client’s VAT details - GOV.UK"
          }

          "not render breadcrumbs which" in {
            an[TestFailedException] should be thrownBy elementText(Selectors.btaBreadcrumb)
            an[TestFailedException] should be thrownBy element(Selectors.btaBreadcrumbLink)
            an[TestFailedException] should be thrownBy elementText(Selectors.vatBreadcrumb)
            an[TestFailedException] should be thrownBy element(Selectors.vatBreadcrumbLink)
          }

          "render back link" in {
            elementText(Selectors.backLink) shouldBe "Back"
          }

          "not display the notice about recently submitted returns" in {
            elementExtinct(Selectors.recentlySubmittedReturnNotice)
          }
        }
      }
    }

    "there is a submitted return in session" should {

      lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
        Seq(
          ReturnObligationsViewModel(
            LocalDate.parse("2018-01-01"),
            LocalDate.parse("2018-03-31"),
            "#001"
          ),
          ReturnObligationsViewModel(
            LocalDate.parse("2018-04-01"),
            LocalDate.parse("2018-06-30"),
            "#002"
          )
        )

      lazy val view: Html = injectedView(
        VatReturnsViewModel(
          returnYears = Seq(2018),
          obligations = exampleReturns,
          showPreviousReturnsTab = false,
          vrn = vrn
        ),
        showInsolvencyContent = false,
        recentlySubmittedReturn = true
      )(fakeRequestWithClientsVRN, messages, mockConfig, agentUser)

      lazy implicit val document: Document = Jsoup.parse(view.body)

      "display the notice about recently submitted returns" in {
        elementText(Selectors.recentlySubmittedReturnNotice) shouldBe "Check back later if you cannot see a recently submitted return."
      }

    }
  }

  "Rendering the Submitted Returns page with a HMRC-MTD-VAT enrolment and a HMCE-VATDEC-ORG enrolment" when {

    "there is no recently submitted return in session" when {

      "one year is retrieved" when {

        val returnYears = Seq(2018)

        lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
          Seq(
            ReturnObligationsViewModel(
              LocalDate.parse("2018-01-01"),
              LocalDate.parse("2018-03-31"),
              "#001"
            ),
            ReturnObligationsViewModel(
              LocalDate.parse("2018-04-01"),
              LocalDate.parse("2018-06-30"),
              "#002"
            )
          )

        "the current year is 2018" should {

          lazy val view = injectedView(
            VatReturnsViewModel(returnYears, exampleReturns, showPreviousReturnsTab = true, vrn),
            showInsolvencyContent = false,
            recentlySubmittedReturn = false
          )

          lazy implicit val document: Document = Jsoup.parse(view.body)

          "have a tab for 2018" in {
            elementText(Selectors.tabOne) should include("2018")
          }

          "have a tab for 'Previous returns'" in {
            elementText(Selectors.tabTwo) should include("Previous returns")
          }

          "display the 'Previous returns' content" which {

            "has the correct h1" in {
              elementText(Selectors.prevReturnsHeading) shouldBe "Service has closed"
            }

            "has the correct text body" in {
              elementText(Selectors.prevReturnsParagraph) shouldBe
                "You can no longer view previous returns. Please refer to your own business records and accounts."
            }
          }

          "not display the notice about recently submitted returns" in {
            elementExtinct(Selectors.recentlySubmittedReturnNotice)
          }
        }
      }

      "two return years are retrieved" when {

        "the current year is 2020" should {

          val returnYears = Seq(2020, 2019)

          lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
            Seq(
              ReturnObligationsViewModel(
                LocalDate.parse("2020-01-01"),
                LocalDate.parse("2020-03-31"),
                "#001"
              ),
              ReturnObligationsViewModel(
                LocalDate.parse("2020-04-01"),
                LocalDate.parse("2020-06-30"),
                "#002"
              ),
              ReturnObligationsViewModel(
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2019-03-31"),
                "#001"
              ),
              ReturnObligationsViewModel(
                LocalDate.parse("2019-04-01"),
                LocalDate.parse("2019-06-30"),
                "#002"
              )
            )

          lazy val view = injectedView(
            VatReturnsViewModel(returnYears, exampleReturns, showPreviousReturnsTab = true, vrn),
            showInsolvencyContent = false,
            recentlySubmittedReturn = false
          )

          lazy implicit val document: Document = Jsoup.parse(view.body)

          "have a tab for 2020" in {
            elementText(Selectors.tabOne) should include("2020")
          }

          "have a tab for 2019" in {
            elementText(Selectors.tabTwo) should include("2019")
          }

          "have a tab for 'Previous Returns'" in {
            elementText(Selectors.tabThree) should include("Previous returns")
          }

          "display the 'Previous returns' content" which {

            "has the correct h1" in {
              elementText(Selectors.prevReturnsHeading) shouldBe "Service has closed"
            }

            "has the correct text body" in {
              elementText(Selectors.prevReturnsParagraph) shouldBe
                "You can no longer view previous returns. Please refer to your own business records and accounts."
            }
          }

          "not display the notice about recently submitted returns" in {
            elementExtinct(Selectors.recentlySubmittedReturnNotice)
          }
        }
      }
    }
  }

  "more than two return years are retrieved" when {

    "there is no recently submitted return in session" when {

      "the current year is 2020" should {

        val returnYears = Seq(2020, 2019, 2018)

        lazy val exampleReturns: Seq[ReturnObligationsViewModel] =
          Seq(
            ReturnObligationsViewModel(
              LocalDate.parse("2020-01-01"),
              LocalDate.parse("2020-03-31"),
              "#001"
            ),
            ReturnObligationsViewModel(
              LocalDate.parse("2020-04-01"),
              LocalDate.parse("2020-06-30"),
              "#002"
            ),
            ReturnObligationsViewModel(
              LocalDate.parse("2019-01-01"),
              LocalDate.parse("2019-03-31"),
              "#001"
            ),
            ReturnObligationsViewModel(
              LocalDate.parse("2019-04-01"),
              LocalDate.parse("2019-06-30"),
              "#002"
            ),
            ReturnObligationsViewModel(
              LocalDate.parse("2018-01-01"),
              LocalDate.parse("2018-03-31"),
              "#001"
            ),
            ReturnObligationsViewModel(
              LocalDate.parse("2018-04-01"),
              LocalDate.parse("2018-06-30"),
              "#002"
            )
          )

        lazy val view = injectedView(
          VatReturnsViewModel(returnYears, exampleReturns, showPreviousReturnsTab = true, vrn),
          showInsolvencyContent = false,
          recentlySubmittedReturn = false
        )

        lazy implicit val document: Document = Jsoup.parse(view.body)

        "have a tab for 2020" in {
          elementText(Selectors.tabOne) should include("2020")
        }

        "have a tab for 2019" in {
          elementText(Selectors.tabTwo) should include("2019")
        }

        "have a tab for 2018" in {
          elementText(Selectors.tabThree) should include("2018")
        }

        "have a tab for 'Previous Returns'" in {
          elementText(Selectors.tabFour) should include("Previous returns")
        }

        "display the 'Previous returns' content" which {

          "has the correct h1" in {
            elementText(Selectors.prevReturnsHeading) shouldBe "Service has closed"
          }

          "has the correct text body" in {
            elementText(Selectors.prevReturnsParagraph) shouldBe
              "You can no longer view previous returns. Please refer to your own business records and accounts."
          }
        }

        "not display the notice about recently submitted returns" in {
          elementExtinct(Selectors.recentlySubmittedReturnNotice)
        }
      }
    }
  }

  "Rendering the submitted returns page for an agent" when {

    "there is no recently submitted return in session" should {

      val returnYears = Seq(2020, 2019, 2018)

      lazy val exampleReturns: Seq[ReturnObligationsViewModel] = Seq(
        ReturnObligationsViewModel(
          LocalDate.parse("2020-01-01"),
          LocalDate.parse("2020-03-31"),
          "#001"
        ),
        ReturnObligationsViewModel(
          LocalDate.parse("2020-04-01"),
          LocalDate.parse("2020-06-30"),
          "#002"
        ),
        ReturnObligationsViewModel(
          LocalDate.parse("2019-01-01"),
          LocalDate.parse("2019-03-31"),
          "#001"
        ),
        ReturnObligationsViewModel(
          LocalDate.parse("2019-04-01"),
          LocalDate.parse("2019-06-30"),
          "#002"
        )
      )

      lazy val view = injectedView(
        VatReturnsViewModel(returnYears, exampleReturns, showPreviousReturnsTab = true, vrn),
        showInsolvencyContent = false,
        recentlySubmittedReturn = false
      )(fakeRequestWithClientsVRN, messages, mockConfig, agentUser)

      lazy implicit val document: Document = Jsoup.parse(view.body)

      "have a tab for 2020" in {
        elementText(Selectors.tabOne) should include("2020")
      }

      "have a tab for 2019" in {
        elementText(Selectors.tabTwo) should include("2019")
      }

      "have a tab for 2018" in {
        elementText(Selectors.tabThree) should include("2018")
      }

      "have a tab for 'Previous Returns'" in {
        elementText(Selectors.tabFour) should include("Previous returns")
      }

      "display the 'Previous returns' content" which {

        "has the correct h1" in {
          elementText(Selectors.prevReturnsHeading) shouldBe "Service has closed"
        }

        "has the correct text body" in {
          elementText(Selectors.prevReturnsParagraph) shouldBe
            "You can no longer view previous returns. Please refer to your own business records and accounts."
        }
      }

      "display a 'Back to client's details' button" in {
        elementText("#finish") shouldBe "Back to client’s details"
        element("#finish > a").attr("href") shouldBe mockConfig.agentClientHubUrl
      }

      "not display the notice about recently submitted returns" in {
        elementExtinct(Selectors.recentlySubmittedReturnNotice)
      }
    }

    "there is a recently submitted return in session" should {

      val returnYears = Seq(2020, 2019, 2018)

      lazy val exampleReturns: Seq[ReturnObligationsViewModel] = Seq(
        ReturnObligationsViewModel(
          LocalDate.parse("2020-01-01"),
          LocalDate.parse("2020-03-31"),
          "#001"
        ),
        ReturnObligationsViewModel(
          LocalDate.parse("2020-04-01"),
          LocalDate.parse("2020-06-30"),
          "#002"
        ),
        ReturnObligationsViewModel(
          LocalDate.parse("2019-01-01"),
          LocalDate.parse("2019-03-31"),
          "#001"
        ),
        ReturnObligationsViewModel(
          LocalDate.parse("2019-04-01"),
          LocalDate.parse("2019-06-30"),
          "#002"
        )
      )

      lazy val view = injectedView(
        VatReturnsViewModel(returnYears, exampleReturns, showPreviousReturnsTab = true, vrn),
        showInsolvencyContent = false,
        recentlySubmittedReturn = true
      )(fakeRequestWithClientsVRN, messages, mockConfig, agentUser)

      lazy implicit val document: Document = Jsoup.parse(view.body)

      "display the notice about recently submitted returns" in {
        elementText(Selectors.recentlySubmittedReturnNotice) shouldBe "Check back later if you cannot see a recently submitted return."
      }
    }
  }

  "The webchat link is displayed" when {
    "the webchatEnabled feature switch is switched on for principal user" in {

      mockConfig.features.webchatEnabled(true)

      lazy val view: Html = injectedView(
          VatReturnsViewModel(
            returnYears = Seq(2018),
            obligations = exampleReturns,
            showPreviousReturnsTab = false,
            vrn = vrn
          ),
          showInsolvencyContent = false,
          recentlySubmittedReturn = false
        )


      lazy implicit val document: Document = Jsoup.parse(view.body)

      document.select("#webchatLink-id").text() shouldBe "Ask HMRC (opens in a new tab)"
      document.select("#webchatLink-id").attr("href") shouldBe "/ask-hmrc/chat/vat-online?ds"
    }

    "the webchatEnabled feature switch is switched on for an agent" in {
      mockConfig.features.webchatEnabled(true)

      lazy val view: Html = injectedView(
        VatReturnsViewModel(
          returnYears = Seq(2018),
          obligations = exampleReturns,
          showPreviousReturnsTab = false,
          vrn = vrn
        ),
        showInsolvencyContent = false,
        recentlySubmittedReturn = false
      )(request, messages, mockConfig, agentUser)


      lazy implicit val document: Document = Jsoup.parse(view.body)

      document.select("#webchatLink-id").text() shouldBe "Ask HMRC (opens in a new tab)"
      document.select("#webchatLink-id").attr("href") shouldBe "/ask-hmrc/chat/vat-online?ds"
    }
  }

  "The webchat link is not displayed" when {
    "the webchatEnabled feature switch is switched off for principal user" in {
      mockConfig.features.webchatEnabled(false)

      lazy val view: Html = injectedView(
        VatReturnsViewModel(
          returnYears = Seq(2018),
          obligations = exampleReturns,
          showPreviousReturnsTab = false,
          vrn = vrn
        ),
        showInsolvencyContent = false,
        recentlySubmittedReturn = false
      )


      lazy implicit val document: Document = Jsoup.parse(view.body)

      document.select("#webchatLink-id").size shouldBe 0
    }

    "the webchatEnabled feature switch is switched off for an agent" in {
      mockConfig.features.webchatEnabled(false)

      lazy val view: Html = injectedView(
        VatReturnsViewModel(
          returnYears = Seq(2018),
          obligations = exampleReturns,
          showPreviousReturnsTab = false,
          vrn = vrn
        ),
        showInsolvencyContent = false,
        recentlySubmittedReturn = false
      )(request, messages, mockConfig, agentUser)


      lazy implicit val document: Document = Jsoup.parse(view.body)

      document.select("#webchatLink-id").size shouldBe 0
    }
  }
}
