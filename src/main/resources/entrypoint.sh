#!/bin/bash

java -cp kapoeira.jar io.cucumber.core.cli.Main \
  --threads ${THREADS} \
  --glue com.lectra.kapoeira.glue \
  -p pretty \
  -p json:/reports/kapoeira-report.json \
  -p junit:/reports/kapoeira-report.xml \
  -p html:/reports/kapoeira-report.html "$1"
