Feature: producer-avro-file-key-value

  Background:
    Given subject
      | name                 | alias     | format |
      | kapoeira.avrovaluev1 | kapoAlias | avro   |
    Given input topic
      | topic           | alias    | key_type | value_type |
      | topic-avrovalue | topic_in | string   | kapoAlias  |
    And output topic
      | topic           | alias     | key_type | value_type | readTimeoutInSecond |
      | topic-avrovalue | topic_out | string   | kapoAlias  | 10                  |
    And var uuid = call function: uuid

  Scenario: Produce a record
    When records from file with  key and  value  are sent
      | topic_alias | separator | file                              |
      | topic_in    | #         | features/records/avrokeyvalue.dat |
    Then expected records
      | topic_alias | key          | value       |
      | topic_out   | key1_${uuid} | aliasValue1 |
      | topic_out   | key2_${uuid} | aliasValue2 |
    And assert aliasValue1 $.anInt == 1
    And assert aliasValue1 $.aString == "myString1"
    And assert aliasValue1 $.anOptionalString == "test"
    And assert aliasValue2 $.anInt == 2
    And assert aliasValue2 $.aString == "myString2"
    And assert aliasValue2 $.anOptionalString == null

