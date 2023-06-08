#!/bin/bash

set -e

kafka-topics --bootstrap-server kafka:29092 --create --topic topic-avrokeyvalue --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server kafka:29092 --create --topic topic-avrovalue --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server kafka:29092 --create --topic topic-string --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server kafka:29092 --create --topic topic-mergejson --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server kafka:29092 --create --topic topic-jsonschema --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server kafka:29092 --create --topic topic-simple-value --partitions 1 --replication-factor 1
kafka-topics --bootstrap-server kafka:29092 --create --topic topic-upper-case-string --partitions 1 --replication-factor 1