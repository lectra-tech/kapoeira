#!/bin/bash

set -e

kafka-topics --bootstrap-server kafka:29092 --create --topic private.euw.kapoeira-dsl-it.fullavro.tracking.raw --partitions 5 --replication-factor 1
kafka-topics --bootstrap-server kafka:29092 --create --topic private.euw.kapoeira-dsl-it.avrovalue.tracking.raw --partitions 5 --replication-factor 1
kafka-topics --bootstrap-server kafka:29092 --create --topic private.euw.kapoeira-dsl-it.stringvalue.tracking.raw --partitions 5 --replication-factor 1
kafka-topics --bootstrap-server kafka:29092 --create --topic private.euw.kapoeira-dsl-it.mergedstringvalue.tracking.raw --partitions 5 --replication-factor 1
kafka-topics --bootstrap-server kafka:29092 --create --topic private.euw.kapoeira-dsl-it.jsonvalue.tracking.raw --partitions 5 --replication-factor 1
