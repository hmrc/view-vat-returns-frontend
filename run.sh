#!/usr/bin/env bash
sbt "run 9151 -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes -Dlogger.resource=logback-test.xml"