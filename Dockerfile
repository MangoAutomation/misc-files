FROM adoptopenjdk:13-jdk-hotspot
MAINTAINER Jared Wiltshire <jared@infiniteautomation.com>

RUN mkdir -p /opt/mango \
    && curl -so /tmp/m2m2-core.zip https://builds.mangoautomation.net/m2m2-core-main.zip \
    && cd /opt/mango \
    && jar xf /tmp/m2m2-core.zip \
    && rm  /tmp/m2m2-core.zip

ENV MA_HOME /opt/mango
ENV CLASSPATH $MA_HOME/overrides/classes:$MA_HOME/classes:$MA_HOME/overrides/properties:$MA_HOME/overrides/lib/*:$MA_HOME/lib/*

EXPOSE 8080
WORKDIR /opt/mango
ENTRYPOINT exec java -server com.serotonin.m2m2.Main
