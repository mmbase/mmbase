ARG PROJECT_VERSION
FROM ghcr.io/mmbase/tomcat:latest-jdk25

USER root

RUN mv /catalina-base/webapps/ROOT/WEB-INF/lib/hsqldb-*.jar  ${CATALINA_BASE}/lib/hsqldb-${HSQLDB_VERSION}.jar

USER mmbase
LABEL org.mmbase.authors="michiel@mmbase.org"
LABEL org.mmbase.description="Tomcat with the MMBase Example Webapp"
