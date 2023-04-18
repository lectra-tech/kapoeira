Feature: consumer-file-key-value

  Background:
    Given input topic
      | topic                                                | alias    | key_type | value_type |
      | private.euw.kapoeira-dsl-it.stringvalue.tracking.raw | topic_in | string   | string     |
    And output topic
      | topic                                                | alias     | key_type | value_type | readTimeoutInSecond |
      | private.euw.kapoeira-dsl-it.stringvalue.tracking.raw | topic_out | string   | string     | 8                   |
    And var uuid = call function : uuid

  Scenario: Produce a record
    When records from file with  key and  value  are sent
      | topic_alias | separator | file                          |
      | topic_in    | #         | features/records/keyvalue.dat |
    Then expected records
      | topic_alias | key          | value       |
      | topic_out   | key1_${uuid} | aliasValue1 |
      | topic_out   | key1_${uuid} | aliasValue2 |
      | topic_out   | key2_${uuid} | aliasValue3 |
      | topic_out   | key5_${uuid} | aliasValue4 |
    And assert aliasValue1 $ == "value1.1"
    And assert aliasValue2 $ == "value1.2"
    And assert aliasValue3 $ == "value2"
