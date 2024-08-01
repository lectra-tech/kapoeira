FROM eclipse-temurin:21.0.4_7-jdk AS builder

ENV SCALA_VERSION 2.13.14
ENV SBT_VERSION 1.10.1

# Install tools...
WORKDIR /opt/tools
# scala
RUN curl -fsL https://downloads.typesafe.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz | tar xfz - -C .
# sbt
RUN curl -fsL https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz | tar xfz - -C .

# PATH
ENV PATH "${PATH}:/opt/tools/scala-${SCALA_VERSION}/bin:/opt/tools/sbt/bin"

# Test
WORKDIR /tmp/test-tools
RUN javac -version
RUN java -version
RUN scalac -version
RUN scala -version
RUN sbt -v sbtVersion -Dsbt.rootdir=true
RUN chmod -R 777 /tmp

WORKDIR /root

COPY build.sbt .
COPY project/ ./project/
RUN sbt update
COPY .git .git
COPY src/ ./src/
RUN sbt clean coverageOn test coverageReport coverageOff
RUN sbt "set assembly / test  := {}" assembly
RUN sbt dependencyUpdatesReport

FROM eclipse-temurin:21-jre AS release
ENV KAFKA_BOOTSTRAP_SERVERS "localhost:9092"
ENV KAFKA_USERNAME ""
ENV KAFKA_PASSWORD ""
ENV KAFKA_SCHEMA_REGISTRY_URL "http://localhost:8081"
ENV KAFKA_SCHEMA_REGISTRY_BASIC_AUTH_CREDENTIALS_SOURCE ""
ENV KAFKA_SCHEMA_REGISTRY_KEY ""
ENV KAFKA_SCHEMA_REGISTRY_SECRET ""

ENV CONFIG_FILE "application.conf"
# example KAPOEIRA_JAVA_SYSTEM_PROPERTIES="-Dkey1=value1 -Dkey2=value2"
ENV KAPOEIRA_JAVA_SYSTEM_PROPERTIES ""
ENV KAPOEIRA_LOGGING_LEVEL "INFO"
ENV KAPOEIRA_THREADS 8

ENV CUCUMBER_PUBLISH_QUIET "true"

## DOCKER INSTALLATION
RUN apt-get update && apt-get -y upgrade && apt-get -y install \
    git \
    wget \
    jq \
    zip \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    gnupg-agent \
    software-properties-common
RUN install -m 0755 -d /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    chmod a+r /etc/apt/keyrings/docker.gpg
RUN echo \
      "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
      "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
      tee /etc/apt/sources.list.d/docker.list > /dev/null
RUN apt-get update && apt-get -y install docker-ce-cli
RUN groupadd docker
RUN usermod -aG docker root

## JAR
COPY --from=builder /root/target/scala-2.13/kapoeira.jar .
## SCRIPT
COPY src/main/resources/entrypoint.sh .
RUN chmod a+x entrypoint.sh

VOLUME /conf
VOLUME /features
VOLUME /reports

ENTRYPOINT ["./entrypoint.sh"]
CMD ["features"]
