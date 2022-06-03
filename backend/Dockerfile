# DISCLAIMER: This Docker file is for demo/dev purposes only!

FROM openjdk:17-jdk-slim-buster

RUN apt-get update -y; apt-get install -y curl unzip

ENV SBT_VERSION 1.6.2
RUN curl -L -o sbt-$SBT_VERSION.zip https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.zip
RUN unzip sbt-$SBT_VERSION.zip -d ops

WORKDIR /app
ADD . /app

EXPOSE 8080

CMD /ops/sbt/bin/sbt "restApi / run"