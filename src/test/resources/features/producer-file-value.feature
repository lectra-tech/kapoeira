Feature: producer-file-value

  Background:
    Given input topic
      | topic        | alias    | key_type | value_type |
      | topic-string | topic_in | string   | string     |
    And output topic
      | topic        | alias     | key_type | value_type | readTimeoutInSecond |
      | topic-string | topic_out | string   | string     | 5                   |
    And var uuid = call function: uuid

  Scenario: Produce a record
    When records from file   with value are sent
      | topic_alias | key          | file                       |
      | topic_in    | keyX_${uuid} | features/records/value.dat |
    Then expected records
      | topic_alias | key          | value  |
      | topic_out   | keyX_${uuid} | valueA |

