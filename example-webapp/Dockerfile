ARG PROJECT_VERSION
FROM ghcr.io/mmbase/tomcat:latest-jdk25

USER root
# would only b needed if database managed by tomcat itself.
#RUN mv /catalina-base/webapps/ROOT/WEB-INF/lib/hsqldb-*.jar  ${CATALINA_BASE}/lib/

USER mmbase
LABEL org.mmbase.authors="michiel@mmbase.org"
LABEL org.mmbase.description="Tomcat with the MMBase Example Webapp"
