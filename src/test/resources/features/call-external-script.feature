Feature: call-external-script

  Background:
    Given input topic
      | topic                                                | alias    | key_type | value_type |
      | private.euw.kapoeira-dsl-it.stringvalue.tracking.raw | topic_in | string   | string     |
    And output topic
      | topic                                                | alias     | key_type | value_type | readTimeoutInSecond |
      | private.euw.kapoeira-dsl-it.stringvalue.tracking.raw | topic_out | string   | string     | 5                   |

  Scenario: call-scripts
    Given var foo = bar
    Given var foo2 = "bar"
    When call script : /features/scripts/runExternalTool.sh 42
    And call script :
    """
    echo 42
    """
    And var myValue = call script : /features/scripts/runExternalTool.sh 43
    And var myKey = call script :
    """
    echo 44
    """
    And var myObject = {"foo":{"bar":"baz"},"qux":42}
    And var myArray = [true,false,null,0,{"foo":"bar"},[1,2,3]]
    And assert var myKey $ == 44
    And assert var myValue $ == "Hello World 43"
    And assert var myObject $.qux == 42
    And assert var myArray $ has size 6
    And assert var myArray $[0] == true
    And assert var myArray $[4].foo == "bar"
    And assert var myArray $[4].foo == "${foo}"
    And assert var myArray $[4].foo == ${foo2}
    And assert var myArray $[5][0] == 1
    And assert var myObject $.foo match object {"bar":"baz"}
    And assert var myObject $ match object {"foo":{"bar":"baz"}}
    And assert var myObject $.foo match exact object {"bar":"baz"}
    And assert var myObject $ match exact object {"foo":{"bar":"baz"},"qux":42}
