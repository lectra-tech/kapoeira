FROM eclipse-temurin:25.0.1_8-jdk AS builder

COPY build.sbt /tmp/build.sbt
COPY project/build.properties /tmp/build.properties
RUN echo "$(grep "scalaVersion" /tmp/build.sbt | cut -d"=" -f2 | xargs)" > /tmp/scalaVersion.txt
RUN echo "$(grep "sbt.version" /tmp/build.properties | cut -d"=" -f2 | xargs)" > /tmp/sbtVersion.txt
RUN rm /tmp/build.sbt /tmp/build.properties

# Install tools...
RUN apt-get update && apt-get -y upgrade && apt-get -y install curl
WORKDIR /opt/tools
# scala
RUN curl -fsL https://github.com/scala/scala/releases/download/v$(cat /tmp/scalaVersion.txt)/scala-$(cat /tmp/scalaVersion.txt).tgz | tar xfz - -C .
RUN mv /opt/tools/scala-$(cat /tmp/scalaVersion.txt) /opt/tools/scala
# sbt
RUN curl -fsL https://github.com/sbt/sbt/releases/download/v$(cat /tmp/sbtVersion.txt)/sbt-$(cat /tmp/sbtVersion.txt).tgz | tar xfz - -C .

# PATH
ENV PATH="${PATH}:/opt/tools/scala/bin:/opt/tools/sbt/bin"

# Test
WORKDIR /tmp/test-tools
RUN javac -version
RUN java -version
RUN scalac -version
RUN scala -version
RUN sbt -v sbtVersion -Dsbt.rootdir=true --allow-empty
RUN chmod -R 777 /tmp

WORKDIR /root

COPY build.sbt .
COPY project/ ./project/
RUN sbt update
COPY .git .git
COPY src/ ./src/
RUN sbt clean coverageOn test coverageReport coverageOff
RUN sbt assembly
RUN sbt dependencyUpdatesReport

FROM eclipse-temurin:25.0.1_8-jre AS release
ENV KAFKA_BOOTSTRAP_SERVERS="localhost:9092"
ENV KAFKA_USERNAME=""
ENV KAFKA_PASSWORD=""
ENV KAFKA_SCHEMA_REGISTRY_URL="http://localhost:8081"
ENV KAFKA_SCHEMA_REGISTRY_BASIC_AUTH_CREDENTIALS_SOURCE=""
ENV KAFKA_SCHEMA_REGISTRY_KEY=""
ENV KAFKA_SCHEMA_REGISTRY_SECRET=""

ENV CONFIG_FILE="application.conf"
# example KAPOEIRA_JAVA_SYSTEM_PROPERTIES="-Dkey1=value1 -Dkey2=value2"
ENV KAPOEIRA_JAVA_SYSTEM_PROPERTIES=""
ENV KAPOEIRA_LOGGING_LEVEL="INFO"
ENV KAPOEIRA_THREADS=8

ENV CUCUMBER_PUBLISH_QUIET="true"

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
