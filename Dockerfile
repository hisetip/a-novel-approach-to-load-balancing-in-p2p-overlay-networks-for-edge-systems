FROM openjdk:14-alpine AS build

RUN apk update && apk add bash
RUN apk add --no-cache \
                bind-tools \
                iproute2 \
                nload
RUN apk add --no-cache python3 py3-pip

COPY docker/* ./
COPY target/asdProj.jar ./asdProj.jar
COPY lib/babel-core-0.4.25.jar ./babel-core-0.4.25.jar
COPY babel-config ./babel-config
COPY log4j2.xml .
COPY deploy ./
COPY src/main/java/protocols/app/config ./src/main/java/protocols/app/config
COPY src/main/java/protocols/optimization/resest/config ./src/main/java/protocols/optimization/resest/config
COPY src/main/java/protocols/optimization/foutakos/config ./src/main/java/protocols/optimization/foutakos/config


ENTRYPOINT ["bash", "./entryF.sh"]