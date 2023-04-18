Feature: producer-file-formatted-value

  Background:
    Given input topic
      | topic                                                | alias    | key_type | value_type |
      | private.euw.kapoeira-dsl-it.stringvalue.tracking.raw | topic_in | string   | string     |
    And output topic
      | topic                                                | alias     | key_type | value_type | readTimeoutInSecond |
      | private.euw.kapoeira-dsl-it.stringvalue.tracking.raw | topic_out | string   | string     | 5                   |
    And var uuid = call function: uuid

  Scenario: Produce a record
    When records from  file with formatted value are sent
      | topic_alias | key          | file                                 |
      | topic_in    | keyY_${uuid} | features/records/formattedvalue.json |
    Then expected records
      | topic_alias | key          | value |
      | topic_out   | keyY_${uuid} | ?     |
