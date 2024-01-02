#!/bin/bash

# FIXME to be removed ASAP
# Deprecated env vars
[[ ${KAFKA_BOOTSTRAP_SERVER:-"unset"} != "unset" ]] && echo "WARN: KAFKA_BOOTSTRAP_SERVER is deprecated, use KAFKA_BOOTSTRAP_SERVERS instead" && export KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVER}
[[ ${KAFKA_USER:-"unset"} != "unset" ]] && echo "WARN: KAFKA_USER is deprecated, use KAFKA_USERNAME instead" && export KAFKA_USERNAME=${KAFKA_USER}
[[ ${THREADS:-"unset"} != "unset" ]] && echo "WARN: THREADS is deprecated, use KAPOEIRA_THREADS instead" && export KAPOEIRA_THREADS=${THREADS}
[[ ${LOGGING_LEVEL:-"unset"} != "unset" ]] && echo "WARN: LOGGING_LEVEL is deprecated, use KAPOEIRA_LOGGING_LEVEL instead" && export KAPOEIRA_LOGGING_LEVEL=${LOGGING_LEVEL}

java -cp kapoeira.jar:/conf -Dconfig.resource=${CONFIG_FILE:-application.conf} ${KAPOEIRA_JAVA_SYSTEM_PROPERTIES} io.cucumber.core.cli.Main \
  --threads ${KAPOEIRA_THREADS:-8} \
  --glue com.lectra.kapoeira.glue \
  -p pretty \
  -p json:/reports/kapoeira-report.json \
  -p junit:/reports/kapoeira-report.xml \
  -p html:/reports/kapoeira-report.html "$1"
