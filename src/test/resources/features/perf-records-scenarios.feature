Feature: consumer-file-key-value

  Background:
    Given input topic
      | topic        | alias    | key_type | value_type |
      | topic-string | topic_in | string   | string     |

    And output topic
      | topic        | alias     | key_type | value_type | readTimeoutInSecond |
      | topic-string | topic_out | string   | string     | 5                   |

  Scenario Outline: Produce several records
    Given var uuid = call function : uuid
    And var suffix = <scenario>

    When records from file with key and value  are sent
      | topic_alias | separator | file                               |
      | topic_in    | #         | features/records/perf-keyvalue.dat |

    Then expected records
      | topic_alias | key          | value        |
      | topic_out   | key1_${uuid} | aliasValue10 |
      | topic_out   | key1_${uuid} | aliasValue11 |
      | topic_out   | key1_${uuid} | aliasValue12 |
      | topic_out   | key1_${uuid} | aliasValue13 |
      | topic_out   | key1_${uuid} | aliasValue14 |
      | topic_out   | key1_${uuid} | aliasValue15 |
      | topic_out   | key1_${uuid} | aliasValue16 |
      | topic_out   | key1_${uuid} | aliasValue17 |
      | topic_out   | key1_${uuid} | aliasValue18 |
      | topic_out   | key1_${uuid} | aliasValue19 |
      | topic_out   | key2_${uuid} | aliasValue20 |
      | topic_out   | key2_${uuid} | aliasValue21 |
      | topic_out   | key2_${uuid} | aliasValue22 |
      | topic_out   | key2_${uuid} | aliasValue23 |
      | topic_out   | key2_${uuid} | aliasValue24 |
      | topic_out   | key2_${uuid} | aliasValue25 |
      | topic_out   | key2_${uuid} | aliasValue26 |
      | topic_out   | key2_${uuid} | aliasValue27 |
      | topic_out   | key2_${uuid} | aliasValue28 |
      | topic_out   | key2_${uuid} | aliasValue29 |
    And assert aliasValue10 $ == "value10_${suffix}"
    And assert aliasValue11 $ == "value11_${suffix}"
    And assert aliasValue12 $ == "value12_${suffix}"
    And assert aliasValue13 $ == "value13_${suffix}"
    And assert aliasValue14 $ == "value14_${suffix}"
    And assert aliasValue15 $ == "value15_${suffix}"
    And assert aliasValue16 $ == "value16_${suffix}"
    And assert aliasValue17 $ == "value17_${suffix}"
    And assert aliasValue18 $ == "value18_${suffix}"
    And assert aliasValue19 $ == "value19_${suffix}"
    And assert aliasValue20 $ == "value20_${suffix}"
    And assert aliasValue21 $ == "value21_${suffix}"
    And assert aliasValue22 $ == "value22_${suffix}"
    And assert aliasValue23 $ == "value23_${suffix}"
    And assert aliasValue24 $ == "value24_${suffix}"
    And assert aliasValue25 $ == "value25_${suffix}"
    And assert aliasValue26 $ == "value26_${suffix}"
    And assert aliasValue27 $ == "value27_${suffix}"
    And assert aliasValue28 $ == "value28_${suffix}"
    And assert aliasValue29 $ == "value29_${suffix}"

    Examples:
      | scenario |
      | 0        |
      | 1        |
      | 2        |
      | 3        |
      | 4        |
      | 5        |
      | 6        |
      | 7        |
      | 8        |
      | 9        |

