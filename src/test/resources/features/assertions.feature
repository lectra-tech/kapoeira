Feature: assertions

  Background:
    Given input topic
      | topic        | alias    | key_type | value_type |
      | topic-string | topic_in | string   | string     |

    And output topic
      | topic        | alias     | key_type | value_type | readTimeoutInSecond |
      | topic-string | topic_out | string   | string     | 5                   |
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

  Scenario: Produce a complex record
    When records from file with  key and  value  are sent
      | topic_alias | separator | file                                       |
      | topic_in    | #         | features/records/keyvalueobjectNarrays.dat |
    Then expected records
      | topic_alias | key          | value  |
      | topic_out   | key1_${uuid} | aValue |
    And assert aValue $.foo == "fooString"
    And assert aValue $.fooInt == 42
    And assert aValue $.foos has size 3
    And assert aValue $.foos == ["item1","item2","item3"]
    And assert aValue $ match object {"foos":["item1","item2","item3"],"bar":{"baz":["item1","item2","item3"]}}
    And assert aValue $ match object {"foos":["item1","item2","item3"]}
    And assert aValue $ match object {"bar":{"baz":["item1","item2","item3"]}}
    And assert aValue $.bar match object {"baz":["item1","item2","item3"]}
    And assert aValue $.bar.baz[0] == "item1"
    And assert aValue $ match exact object {"foo":"fooString","fooInt":42,"foos":["item1","item2","item3"],"qux":[{"key1":"toto"},{"key2":"titi"}],"bar":{"baz":["item1","item2","item3"]}}
    And assert aValue $.bar match exact object {"baz":["item1","item2","item3"]}
    And assert aValue $.qux[?(@.key1!=null)] match object {"key1":"toto"}
