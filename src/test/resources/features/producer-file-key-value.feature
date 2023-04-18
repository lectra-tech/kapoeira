Feature: producer-file-key-value

  Background:
    Given input topic
      | topic                                                | alias    | key_type | value_type |
      | private.euw.kapoeira-dsl-it.stringvalue.tracking.raw | topic_in | string   | string     |
    And output topic
      | topic                                                | alias     | key_type | value_type | readTimeoutInSecond |
      | private.euw.kapoeira-dsl-it.stringvalue.tracking.raw | topic_out | string   | string     | 5                   |
    And var uuid = call function: uuid


  Scenario: Produce a record with headers
    When records from file with  key  and  value  are sent
      | topic_alias | separator | file                                 |
      | topic_in    | #         | features/records/keyheadersvalue.dat |
    Then expected records
      | topic_alias | key          | headers         | value    |
      | topic_out   | key1_${uuid} | aliasHeaders2.1 | value2.1 |
      | topic_out   | key2_${uuid} | aliasHeaders2.2 | value2.2 |
      | topic_out   | key3_${uuid} | aliasHeaders2.3 | value2.3 |
    And assert value2.1 $.qux == 42
    And assert value2.2 $ has size 2
    And assert value2.2 $ == [3,4]
    And assert value2.3 $ == "value2.3"
    And assert aliasHeaders2.1 $ == {"foo":"bar","baz":"42"}
    And assert aliasHeaders2.1 $.foo == "bar"

