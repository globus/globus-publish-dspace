#!/bin/sh

mvn install:install-file -Dfile=handle-lib-7.3.1.jar -DpomFile=handle-pom.xml -Dsources=handle-client-src.zip
