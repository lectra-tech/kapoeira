FROM eclipse-temurin:17.0.9_9-jdk as builder

ENV SCALA_VERSION 2.13.12
ENV SBT_VERSION 1.9.8

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
COPY version.txt .
RUN sbt update
COPY src/ ./src/
RUN sbt clean coverageOn test coverageReport coverageOff
RUN sbt "set assembly / test  := {}" assembly
RUN sbt dependencyUpdatesReport

FROM eclipse-temurin:17-jre as release
ENV KAFKA_BOOTSTRAP_SERVER "localhost:9092"
ENV KAFKA_SCHEMA_REGISTRY_URL "http://localhost:8081"
ENV KAFKA_USER "xxx-bot"
ENV KAFKA_PASSWORD "XXX"
ENV JAAS_AUTHENT "true"
ENV CONSUMER_TIMOUT_MS 8000
ENV CONSUMER_MAX_MESSAGES 100
ENV DOCKER_API_VERSION 1.39
ENV LOGGING_LEVEL "INFO"
ENV CUCUMBER_PUBLISH_QUIET "true"
ENV THREADS 8

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

VOLUME /features
VOLUME /reports

ENTRYPOINT ["./entrypoint.sh"]
CMD ["features"]
