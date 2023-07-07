#!/usr/bin/env bash

#Simulate the production of an effect that should produce a record back to Kafka

CP_IMAGE_NAME=confluentinc/cp-kafka-connect-base:7.1.1

echo ${@:2:${#}} > /tmp/inputForExtSys

docker run --rm -t \
  -v /tmp/inputForExtSys:/tmp/input/target \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --network local_kafka \
  ${CP_IMAGE_NAME} \
  bash -c "kafka-console-producer \
    --topic ${1} \
    --bootstrap-server 'kafka:9092' \
    --property parse.key=true \
    --property key.serializer=org.apache.kafka.common.serialization.StringSerializer \
    --property key.separator='#' < /tmp/input/target"
