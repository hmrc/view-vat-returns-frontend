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
    "app.*",
    "views.*",
    "prod.*",
    "config.*",
    "testOnly.*"
  )

  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 95,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

val compile = Seq(
  ws,
  "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % "7.12.0",
  "com.typesafe.play" %% "play-json-joda"             % "2.10.0-RC6",
  "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "3.34.0-play-28"
)

def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
  "uk.gov.hmrc"             %% "bootstrap-test-play-28"       % "7.12.0"           % scope,
  "org.jsoup"               %  "jsoup"                        % "1.15.2"          % scope,
  "org.scalamock"           %% "scalamock"                    % "5.2.0"           % scope
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
    scalaVersion := "2.13.8",
    libraryDependencies ++= appDependencies,
    retrieveManaged := true
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    IntegrationTest / Keys.fork := false,
    IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    IntegrationTest / testGrouping := oneForkedJvmPerTest((IntegrationTest / definedTests).value),
    IntegrationTest / parallelExecution := false)
