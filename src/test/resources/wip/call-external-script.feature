Feature: call-external-script

  Background:
    Given input topic
      | topic        | alias    | key_type | value_type |
      | topic-string | topic_in | string   | string     |
    And output topic
      | topic        | alias     | key_type | value_type | readTimeoutInSecond |
      | topic-string | topic_out | string   | string     | 5                   |
    And var uuid = call function: uuid

  Scenario: call-scripts
    Given var foo = bar
    Given var foo2 = "bar"
    When call script : src/test/resources/features/scripts/runExternalTool.sh 42
    And call script :
    """
    echo 42
    """
    And var myValue = call script : src/test/resources/features/scripts/runExternalTool.sh 43
    And var myKey = call script :
    """
    echo 44
    """
    And assert var myKey $ == 44
    And assert var myValue $ == "Hello World 43"

  Scenario: call script after sending events
  It should produce two records : one directly from Kapoeira, and another one from an interaction with an external system
  that produces back to kafka.

    When records with key and value are sent
      | topic_alias | key              | value     |
      | topic_in    | aTestKey_${uuid} | someValue |
    And call script: src/test/resources/features/scripts/externalEffectProducingToKafka.sh topic-string ${uuid}#producedByExternalSystem_${uuid}
    Then expected records
      | topic_alias | key              | value   |
      | topic_out   | aTestKey_${uuid} | aValue1 |
      | topic_out   | aTestKey_${uuid} | aValue2 |
    And assert aValue1 $ == "someValue"
    And assert aValue2 $ == "producedByExternalSystem_${uuid}"
