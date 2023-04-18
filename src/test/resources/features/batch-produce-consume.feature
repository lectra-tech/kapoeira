Feature: producer-file-key-value-mode-batch

  Background:
    Given subject
      | name                 | alias     | format |
      | kapoeira.avrovaluev1 | kapoAlias | avro   |
    Given input topic
      | topic                                                | alias     | key_type | value_type |
      | private.euw.kapoeira-dsl-it.stringvalue.tracking.raw | topic_in1 | string   | string     |
      | private.euw.kapoeira-dsl-it.avrovalue.tracking.raw   | topic_in2 | string   | kapoAlias  |

    And output topic
      | topic                                                | alias     | key_type | value_type | readTimeoutInSecond |
      | private.euw.kapoeira-dsl-it.mergedstringvalue.tracking.raw | topic_out | string   | string     | 5                   |
    And var uuid = call function: uuid

  Scenario: Produce records in multiple topics, using batch mode to keep order between consumption and production
    When records from file with  key  and  value  are sent
      | topic_alias | separator | file                          | batch |
      | topic_in1   | #         | features/records/batch1.1.dat | 1     |
      | topic_in1   | #         | features/records/batch1.2.dat | 1     |
      | topic_in2   | #         | features/records/batch2.1.dat | 2     |
      | topic_in2   | #         | features/records/batch2.2.dat | 2     |
    Then expected records
      | topic_alias | key             | value    | batch |
      | topic_out   | samekey_${uuid} | value1.1 | 1     |
      | topic_out   | samekey_${uuid} | value1.2 | 1     |
      | topic_out   | samekey_${uuid} | value2.1 | 2     |
      | topic_out   | samekey_${uuid} | value2.2 | 2     |
    And assert value1.1 $.FOO == 1_${uuid}
    And assert value1.2 $.FOO == 2_${uuid}
    And assert value1.1 $.ANINT == 3
    And assert value2.2 $.ANINT == 4
