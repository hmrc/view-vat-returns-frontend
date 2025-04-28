#!/usr/bin/env bash
sbt "run 9151 -Dapplication.http.router=testOnlyDoNotUseInAppConf.Routes -Dlogger.resource=logback-test.xml"