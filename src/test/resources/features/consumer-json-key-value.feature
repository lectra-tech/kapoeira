Feature: consumer-json-key-value

  Background:
    Given subject
      | name                 | alias      | format |
      | kapoeira.jsonvaluev1 | json_value | json   |
    And input topic
      | topic            | alias    | key_type | value_type |
      | topic-jsonschema | topic_in | string   | json_value |
    And output topic
      | topic            | alias     | key_type | value_type | readTimeoutInSecond |
      | topic-jsonschema | topic_out | string   | json_value | 10                  |
    And var uuid = call function : uuid

  Scenario: Produce a record
    When records with key and value are sent
      | topic_alias | key              | value                                                            |
      | topic_in    | aTestKey_${uuid} | {"anInt": 1, "aString": "myString1", "anOptionalString": "test"} |
      | topic_in    | aTestKey_${uuid} | {"anInt": 2, "aString": "myString2", "anOptionalString": null }  |
#  the following payload does not respect json schema => the test must fail
#      | topic_in    | aTestKey_${uuid} | {"foo": "bar" }                                                  |
    Then expected records
      | topic_alias | key              | value       |
      | topic_out   | aTestKey_${uuid} | aliasValue1 |
      | topic_out   | aTestKey_${uuid} | aliasValue2 |
#      | topic_out   | aTestKey_${uuid} | aliasValue3 |
    And assert aliasValue1 $.anInt == 1
    And assert aliasValue1 $.aString == "myString1"
    And assert aliasValue1 $.anOptionalString == "test"
    And assert aliasValue2 $.anInt == 2
    And assert aliasValue2 $.aString == "myString2"
    And assert aliasValue2 $.anOptionalString == null
#    And assert aliasValue3 $.foo == "bar"
