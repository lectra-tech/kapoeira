Feature: consumer-key-value

  Background:
    Given input topic
      | topic        | alias    | key_type | value_type |
      | topic-string | topic_in | string   | string     |
    And var foo = 42
    And var bar = 33
    And var key = aTestKey
    And var toto = call function: uuid
    And var uuid = call function: uuid
    And var testJson = {"obj": "hello world" }
    And output topic
      | topic        | alias     | key_type | value_type | readTimeoutInSecond |
      | topic-string | topic_out | string   | string     | 8                   |

  Scenario: Produce a record String/String
    When records with key and value are sent
      | topic_alias | key              | value             |
      | topic_in    | ${key}_${uuid}   | aValue1           |
      | topic_in    | aTestKey_${uuid} | x ${foo} y ${bar} |
      | topic_in    | aTestKey_${uuid} | ${toto}           |
      | topic_in    | aTestKey_${uuid} | ${testJson}       |
    Then expected records
      | topic_alias | key              | value       |
      | topic_out   | aTestKey_${uuid} | aliasValue1 |
      | topic_out   | ${key}_${uuid}   | aliasValue2 |
      | topic_out   | aTestKey_${uuid} | aliasValue3 |
      | topic_out   | aTestKey_${uuid} | aliasValue4 |
    And assert aliasValue1 $ == "aValue1"
    And assert aliasValue2 $ == "x ${foo} y ${bar}"
    And assert aliasValue3 $ == "${toto}"
    And assert aliasValue4 $.obj == "hello world"

  Scenario: Produce a record String/JsonString
    When records with key and value are sent
      | topic_alias | key               | value              |
      | topic_in    | aTestKey_${uuid}  | "aValue"           |
      | topic_in    | aTestKey2_${uuid} | {"a": "aValue2"}   |
      | topic_in    | aTestKey2_${uuid} | [{"a": "aValue3"}] |
    Then expected records
      | topic_alias | key               | value      |
      | topic_out   | aTestKey_${uuid}  | jsonString |
      | topic_out   | aTestKey2_${uuid} | jsonObject |
      | topic_out   | aTestKey2_${uuid} | jsonArray  |
    And assert jsonString $ == "\"aValue\""
    And assert jsonObject $.a == "aValue2"
    And assert jsonArray $[0].a == "aValue3"
