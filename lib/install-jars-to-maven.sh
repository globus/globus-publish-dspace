#!/bin/sh

mvn install:install-file -Dfile=handle-lib-8.1.4.jar -DpomFile=handle-pom.xml -Dsources=handle-client-8.1.4-src.zip
