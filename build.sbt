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

import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import play.sbt.routes.RoutesKeys
import sbt.Tests.{Group, SubProcess}

val appName: String = "view-vat-returns-frontend"
lazy val appDependencies: Seq[ModuleID] = compile ++ test()
lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala)
lazy val playSettings: Seq[Setting[_]] = Seq.empty
RoutesKeys.routesImport := Seq.empty

lazy val coverageSettings: Seq[Setting[_]] = {
  import scoverage.ScoverageKeys

  val excludedPackages = Seq(
    "<empty>",
    ".*Reverse.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "views.html.templates.formatters.*",
    "prod.*",
    "config.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    "com.kenshoo.play.metrics.*",
    "controllers.javascript.*",
    ".*feedback*.*"
  )

  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimum := 95,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

val compile = Seq(
  ws,
  "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % "5.7.0",
  "uk.gov.hmrc"       %% "play-ui"                    % "9.6.0-play-28",
  "uk.gov.hmrc"       %% "play-partials"              % "8.1.0-play-28",
  "uk.gov.hmrc"       %% "play-language"              % "5.1.0-play-28",
  "com.typesafe.play" %% "play-json-joda"             % "2.7.4",
  "uk.gov.hmrc"       %% "play-frontend-govuk"        % "0.80.0-play-28",
  "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "0.83.0-play-28"
)

def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
  "org.scalatest"           %% "scalatest"                    % "3.1.4"           % scope,
  "com.vladsch.flexmark"    %  "flexmark-all"                 % "0.36.8"          % scope,
  "org.pegdown"             %  "pegdown"                      % "1.6.0"           % scope,
  "org.jsoup"               %  "jsoup"                        % "1.14.1"          % scope,
  "org.scalatestplus.play"  %% "scalatestplus-play"           % "5.1.0"           % scope,
  "org.scalamock"           %% "scalamock-scalatest-support"  % "3.6.0"           % scope,
  "com.github.tomakehurst"  %  "wiremock-jre8"                % "2.26.3"          % scope
)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] = tests map {
  test =>
    Group(
      test.name,
      Seq(test),
      SubProcess(ForkOptions().withRunJVMOptions(Vector("-Dtest.name=" + test.name, "-Dlogger.resource=logback-test.xml")))
    )
}

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.govukfrontend.views.html.helpers._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin) ++ plugins : _*)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(PlayKeys.playDefaultPort := 9151)
  .settings(coverageSettings: _*)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(majorVersion := 0)
  .settings(
    scalaVersion := "2.12.12",
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false)
