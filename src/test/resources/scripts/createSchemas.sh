#!/bin/bash

curl --request POST \
  --url http://schema-registry:8081/subjects/kapoeira.avrokeyv1/versions \
  --header 'Content-Type: application/vnd.schemaregistry.v1+json' \
  --data '{"schema": "{\"type\":\"record\",\"name\":\"Avrokeyv1\",\"namespace\":\"com.lectra.kapoeira\",\"fields\":[{\"name\":\"aKey\",\"type\":\"string\"}]}"}'


curl --request POST \
  --url http://schema-registry:8081/subjects/private.euw.kapoeira-dsl-it.fullavro.tracking.raw-key/versions \
  --header 'Content-Type: application/vnd.schemaregistry.v1+json' \
  --data '{"schema": "{\"type\":\"record\",\"name\":\"Avrokeyv1\",\"namespace\":\"com.lectra.kapoeira\",\"fields\":[{\"name\":\"aKey\",\"type\":\"string\"}]}"}'


curl --request POST \
  --url http://schema-registry:8081/subjects/kapoeira.avrovaluev1/versions \
  --header 'Content-Type: application/vnd.schemaregistry.v1+json' \
  --data '{"schema": "{\"type\":\"record\",\"name\":\"Avrovaluev1\",\"namespace\":\"com.lectra.kapoeira\",\"fields\":[{\"name\":\"anInt\",\"type\":\"int\"},{\"name\":\"aString\",\"type\":\"string\"},{\"name\":\"anOptionalString\",\"type\":[\"null\",\"string\"],\"default\":null}]}"}'

curl --request POST \
  --url http://schema-registry:8081/subjects/private.euw.kapoeira-dsl-it.avrovalue.tracking.raw-value/versions \
  --header 'Content-Type: application/vnd.schemaregistry.v1+json' \
  --data '{"schema": "{\"type\":\"record\",\"name\":\"Avrovaluev1\",\"namespace\":\"com.lectra.kapoeira\",\"fields\":[{\"name\":\"anInt\",\"type\":\"int\"},{\"name\":\"aString\",\"type\":\"string\"},{\"name\":\"anOptionalString\",\"type\":[\"null\",\"string\"],\"default\":null}]}"}'

curl --request POST \
  --url http://schema-registry:8081/subjects/private.euw.kapoeira-dsl-it.fullavro.tracking.raw-value/versions \
  --header 'Content-Type: application/vnd.schemaregistry.v1+json' \
  --data '{"schema": "{\"type\":\"record\",\"name\":\"Avrovaluev1\",\"namespace\":\"com.lectra.kapoeira\",\"fields\":[{\"name\":\"anInt\",\"type\":\"int\"},{\"name\":\"aString\",\"type\":\"string\"},{\"name\":\"anOptionalString\",\"type\":[\"null\",\"string\"],\"default\":null}]}"}'


curl --request POST \
  --url http://schema-registry:8081/subjects/kapoeira.jsonvaluev1/versions \
  --header 'Content-Type: application/vnd.schemaregistry.v1+json' \
  --data '{"schemaType": "JSON","schema": "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\", \"title\": \"com.lectra.kapoeira.jsonvaluev1\", \"description\": \"jsonvaluev1\", \"type\": \"object\", \"properties\": { \"anInt\": { \"type\": \"integer\" }, \"aString\": { \"type\": \"string\" }, \"anOptionalString\": {\"type\":[\"string\",\"null\"]} } }"}'
