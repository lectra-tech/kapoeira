Feature: call-external-script

  Background:
    Given input topic
      | topic        | alias    | key_type | value_type |
      | topic-string | topic_in | string   | string     |
    And output topic
      | topic        | alias     | key_type | value_type | readTimeoutInSecond |
      | topic-string | topic_out | string   | string     | 10                  |
    And var uuid = call function: uuid

  Scenario: call-scripts
    Given var foo = bar
    Given var foo2 = "bar"
    When call script : /features/scripts/runExternalTool.sh 42
    And call script :
    """
    echo This a test for simply echoing 42
    """
    And var myValue = call script : /features/scripts/runExternalTool.sh 43
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
      | topic_alias | key              | value     | batch |
      | topic_in    | aTestKey_${uuid} | someValue | 1     |
    And call script: /features/scripts/externalEffectProducingToKafka.sh send topic-string aTestKey2_${uuid}#producedByExternalSystem_${uuid}
    Then expected records
      | topic_alias | key               | value   | batch |
      | topic_out   | aTestKey_${uuid}  | aValue1 | 1     |
      | topic_out   | aTestKey2_${uuid} | aValue2 | 1     |
    And assert aValue1 $ == "someValue"
    And assert aValue2 $ == "producedByExternalSystem_${uuid}"

  Scenario: call script after sending events in multiple batches
  It should produce three records : two directly from Kapoeira, and another one from an interaction with an external system
  that produces back to kafka in the last batch declared.

    When records with key and value are sent
      | topic_alias | key               | value     | batch |
      | topic_in    | aTestKey3_${uuid} | someValue | 1     |
      | topic_in    | aTestKey3_${uuid} | someValue | 2     |
    And call script: /features/scripts/externalEffectProducingToKafka.sh send topic-string aTestKey4_${uuid}#producedByExternalSystem_${uuid}
    Then expected records
      | topic_alias | key               | value   | batch |
      | topic_out   | aTestKey3_${uuid} | aValue1 | 1     |
      | topic_out   | aTestKey3_${uuid} | aValue2 | 2     |
      | topic_out   | aTestKey4_${uuid} | aValue3 | 2     |
    And assert aValue1 $ == "someValue"
    And assert aValue2 $ == "someValue"
    And assert aValue3 $ == "producedByExternalSystem_${uuid}"
