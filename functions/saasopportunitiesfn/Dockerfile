#
# Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
# Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
#

FROM fnproject/fn-java-fdk-build:jdk11-1.0.108 as build-stage
WORKDIR /function

COPY lib /function/lib

# Uncomment this line and populate if you are behind a proxy
# ENV MAVEN_OPTS -Dhttp.proxyHost=<ProxyHost> -Dhttp.proxyPort=<ProxyPort> -Dhttps.proxyHost=<ProxyHost> -Dhttps.proxyPort=<ProxyPort>

ADD lib/idcsOAuthAsserter*.pom /function/pom.xml
RUN ["mvn", "org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file", "-Dfile=/function/lib/idcsOAuthAsserter-1.0.0.jar", "dependency:copy-dependencies", "-DincludeScope=runtime", "-DskipTests=false", "-Dmdep.prependGroupId=true", "-DoutputDirectory=target"]

ADD pom.xml /function/pom.xml
RUN ["mvn", "package", "dependency:copy-dependencies", "-DincludeScope=runtime", "-DskipTests=false", "-Dmdep.prependGroupId=true", "-DoutputDirectory=target"]

ADD src /function/src
RUN ["mvn", "package"]
FROM fnproject/fn-java-fdk:jre11-1.0.108
WORKDIR /function
COPY --from=build-stage /function/target/*.jar /function/app/

CMD ["com.example.saas.fn.cloudnativesaas.SaaSOpportunitiesFunctions::handleRequest"]