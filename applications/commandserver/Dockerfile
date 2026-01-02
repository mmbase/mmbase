ARG TAG=latest

FROM ghcr.io/mmbase/env:8-jdk25

ENV JPDA_ADDRESS="*:8001"

RUN apt-get update && \
    apt-get -y  install ffmpeg && \
    rm -rf /var/lib/apt/lists/*

COPY target/mmbase-commandserver-*.jar /mmbase-commandserver.jar

ENTRYPOINT java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${JPDA_ADDRESS} -jar /mmbase-commandserver.jar 1679


