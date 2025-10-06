/*
 * Copyright 2023 HM Revenue & Customs
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

import play.sbt.routes.RoutesKeys
import uk.gov.hmrc.DefaultBuildSettings.*

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.16"

val appName: String = "view-vat-returns-frontend"
lazy val appDependencies: Seq[ModuleID] = compile ++ test()
lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala)
lazy val playSettings: Seq[Setting[?]] = Seq.empty
RoutesKeys.routesImport := Seq("uk.gov.hmrc.play.bootstrap.binders.RedirectUrl")

lazy val coverageSettings: Seq[Setting[?]] = {
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
  "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % "10.1.0",
  "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % "12.12.0"
)

def test(scope: String = "test"): Seq[ModuleID] = Seq(
  "uk.gov.hmrc"             %% "bootstrap-test-play-30"       % "10.1.0"          % scope,
  "org.scalamock"           %% "scalamock"                    % "7.3.0"           % scope
)

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins((Seq(play.sbt.PlayScala, SbtDistributablesPlugin) ++ plugins) *)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(PlayKeys.playDefaultPort := 9151)
  .settings(coverageSettings *)
  .settings(playSettings *)
  .settings(scalaSettings *)
  .settings(defaultSettings() *)
  .settings(
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
      scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Wconf:cat=unused-imports&site=.*views.html.*:s",
        "-Wconf:cat=unused-imports&src=html/.*:s", "-Wconf:src=routes/.*:s")
  )

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(itSettings())
  .settings(
    fork := false,
    addTestReportOption(Test, "int-test-reports")
  )
