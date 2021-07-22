#!/usr/bin/env bash
sbt "run 9151 -Dplay.http.router=testOnly.Routes -Dlogger.resource=logback-test.xml"