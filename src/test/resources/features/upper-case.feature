Feature: upper-case

  Background:
    Given input topic
      | topic              | alias    | key_type | value_type |
      | topic-simple-value | topic_in | string   | string     |

    And output topic
      | topic                   | alias            | key_type | value_type | readTimeoutInSecond |
      | topic-simple-value      | topic_string_out | string   | string     | 5                   |
      | topic-upper-case-string | topic_out        | string   | string     | 5                   |
    And var myKey = call function: uuid

  Scenario: My first scenario
    When records with key and value are sent
      | topic_alias | key      | value |
      | topic_in    | ${myKey} | a     |
      | topic_in    | ${myKey} | b     |
      | topic_in    | ${myKey} | c     |
    Then expected records
      | topic_alias      | key      | value    |
      | topic_string_out | ${myKey} | input_1  |
      | topic_string_out | ${myKey} | input_2  |
      | topic_string_out | ${myKey} | input_3  |
      | topic_out        | ${myKey} | result_1 |
      | topic_out        | ${myKey} | result_2 |
      | topic_out        | ${myKey} | result_3 |
    And assert input_1 $ == "a"
    And assert input_2 $ == "b"
    And assert input_3 $ == "c"

    And assert result_1 $ == "A"
    And assert result_2 $ == "B"
    And assert result_3 $ == "C"

  Scenario Outline: My outline scenario
    When records with key and value are sent
      | topic_alias | key      | value        |
      | topic_in    | ${myKey} | <inputValue> |
    Then expected records
      | topic_alias      | key      | value  |
      | topic_string_out | ${myKey} | input  |
      | topic_out        | ${myKey} | result |
    And assert input $ == "<inputValue>"
    And assert result $ == "<outputValue>"

    Examples:
      | inputValue | outputValue |
      | a          | A           |
      | b          | B           |
      | c          | C           |
      | d          | D           |
      | kapoeira   | KAPOEIRA    |
      | z          | Z           |


