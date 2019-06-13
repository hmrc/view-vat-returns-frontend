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

import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import play.core.PlayVersion
import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName: String = "view-vat-returns-frontend"
lazy val appDependencies: Seq[ModuleID] = compile ++ test()
lazy val plugins : Seq[Plugins] = Seq(play.sbt.PlayScala)
lazy val playSettings: Seq[Setting[_]] = Seq.empty

lazy val coverageSettings: Seq[Setting[_]] = {
  import scoverage.ScoverageKeys

  val excludedPackages = Seq(
    "<empty>",
    "Reverse.*",
    ".*standardError*.*",
    ".*govuk_wrapper*.*",
    ".*main_template*.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "views.html.templates.formatters.*",
    "prod.*",
    "config.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    ".*feedback*.*")

  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimum := 95,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

val compile = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-play-25" % "4.13.0",
  "uk.gov.hmrc" %% "govuk-template" % "5.35.0-play-25",
  "uk.gov.hmrc" %% "play-ui" % "7.40.0-play-25",
  "uk.gov.hmrc" %% "play-partials" % "6.9.0-play-25",
  "uk.gov.hmrc" %% "play-whitelist-filter" % "3.1.0-play-25",
  "uk.gov.hmrc" %% "play-language" % "3.4.0",
  "org.typelevel" %% "cats" % "0.9.0"
)

def test(scope: String = "test, it"): Seq[ModuleID] = Seq(
  "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25" % scope,
  "org.scalatest" %% "scalatest" % "3.0.1" % scope,
  "org.pegdown" % "pegdown" % "1.6.0" % scope,
  "org.jsoup" % "jsoup" % "1.10.3" % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % scope,
  "com.github.tomakehurst" % "wiremock" % "2.6.0" % scope
)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] = tests map {
  test =>
    Group(
      test.name,
      Seq(test),
      SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name, "-Dlogger.resource=logback-test.xml")))
    )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins : _*)
  .settings(PlayKeys.playDefaultPort := 9151)
  .settings(coverageSettings: _*)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(majorVersion := 0)
  .settings(
    scalaVersion := "2.11.11",
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesGenerator := InjectedRoutesGenerator
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest)(base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest := false)
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo
  ))
