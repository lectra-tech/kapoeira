#!/bin/bash

SCRIPTNAME=$(basename "$0")
RUNDIR=$(dirname "$(realpath $0)")
CP_IMAGE_NAME=confluentinc/cp-kafka-connect-base:7.2.9

function generateInput {
  local tmpFile=$1
  local writeTofile=${@:2:${#}}
  echo  $writeTofile > $tmpFile
}
function console-producer {
  local topic=$1
  local tmpFile=$2
  kafka-console-producer \
        --topic ${topic} \
        --bootstrap-server 'kafka:29092' \
        --property parse.key=true \
        --property key.serializer=org.apache.kafka.common.serialization.StringSerializer \
        --property key.separator='#' < ${tmpFile}
}

#Simulate an effect on a system that will produce a record back to Kafka
function send {
  tmpFile=/tmp/kapoeira/inputForExtSys_$(cat /proc/sys/kernel/random/uuid)
  local topic=$1
  local writeTofile=${@:2:${#}}
  docker run --rm -u root \
    --volumes-from kapoeira-it \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v tmpFileVolume:/tmp/kapoeira \
    --entrypoint bash bash $RUNDIR/$SCRIPTNAME generateInput $tmpFile $writeTofile
  docker run --rm \
    --volumes-from kapoeira-it \
    -v tmpFileVolume:/tmp/kapoeira \
    -v /var/run/docker.sock:/var/run/docker.sock \
    --network local_kafka \
    ${CP_IMAGE_NAME} \
    $RUNDIR/$SCRIPTNAME console-producer $topic $tmpFile
}

$@