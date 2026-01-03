ARG PROJECT_VERSION
FROM ghcr.io/mmbase/tomcat:9-jdk8


USER root
ARG HSQLDB_VERSION=1.8.0.10
RUN  curl --fail -L  \
     https://repo1.maven.org/maven2/hsqldb/hsqldb/${HSQLDB_VERSION}/hsqldb-${HSQLDB_VERSION}.jar -o ${CATALINA_BASE}/lib/hsqldb-${HSQLDB_VERSION}.jar


USER mmbase
LABEL org.mmbase.authors="michiel@mmbase.org"
LABEL org.mmbase.description="Tomcat with the MMBase Base Webapp"
