#!/bin/bash

java -cp kapoeira.jar:/conf -Dconfig.resource=${CONFIG_FILE:-application.conf} ${KAPOEIRA_JAVA_SYSTEM_PROPERTIES} io.cucumber.core.cli.Main \
  --threads ${KAPOEIRA_THREADS:-8} \
  --glue com.lectra.kapoeira.glue \
  -p pretty \
  -p json:/reports/kapoeira-report.json \
  -p junit:/reports/kapoeira-report.xml \
  -p html:/reports/kapoeira-report.html "$1"
