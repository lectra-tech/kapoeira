#!/bin/bash

curl --request POST \
  --url http://schema-registry:8081/subjects/kapoeira.avrokeyv1/versions \
  --header 'Content-Type: application/vnd.schemaregistry.v1+json' \
  --user 'srUsername:srPassword' \
  --data @schemas/kapoeira.avrokeyv1.json

curl --request POST \
  --url http://schema-registry:8081/subjects/topic-avrokeyvalue-key/versions \
  --header 'Content-Type: application/vnd.schemaregistry.v1+json' \
  --user 'srUsername:srPassword' \
  --data @schemas/kapoeira.avrokeyv1.json

curl --request POST \
  --url http://schema-registry:8081/subjects/kapoeira.avrovaluev1/versions \
  --header 'Content-Type: application/vnd.schemaregistry.v1+json' \
  --user 'srUsername:srPassword' \
  --data @schemas/kapoeira.avrovaluev1.json

curl --request POST \
  --url http://schema-registry:8081/subjects/topic-avrovalue-value/versions \
  --header 'Content-Type: application/vnd.schemaregistry.v1+json' \
  --user 'srUsername:srPassword' \
  --data @schemas/kapoeira.avrovaluev1.json

curl --request POST \
  --url http://schema-registry:8081/subjects/topic-avrokeyvalue-value/versions \
  --header 'Content-Type: application/vnd.schemaregistry.v1+json' \
  --user 'srUsername:srPassword' \
  --data @schemas/kapoeira.avrovaluev1.json

curl --request POST \
  --url http://schema-registry:8081/subjects/kapoeira.jsonvaluev1/versions \
  --header 'Content-Type: application/vnd.schemaregistry.v1+json' \
  --user 'srUsername:srPassword' \
  --data @schemas/kapoeira.jsonvaluev1.json
