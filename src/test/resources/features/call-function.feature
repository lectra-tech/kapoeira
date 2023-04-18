Feature: call-functions

  Background:
    Given input topic
      | topic                                                | alias    | key_type | value_type |
      | private.euw.kapoeira-dsl-it.stringvalue.tracking.raw | topic_in | string   | string     |
    And output topic
      | topic                                                | alias     | key_type | value_type | readTimeoutInSecond |
      | private.euw.kapoeira-dsl-it.stringvalue.tracking.raw | topic_out | string   | string     | 5                   |

  Scenario: uuid
    Given var foo = bar
    And var uuid = call function : uuid
    And var isUUID = call script :  /features/scripts/isUUID.sh ${uuid}
    And var isNotUUID = call script :  /features/scripts/isUUID.sh ${foo}
    When records with key and value are sent
      | topic_alias | key       | value        |
      | topic_in    | isUUID    | ${isUUID}    |
      | topic_in    | isNotUUID | ${isNotUUID} |
    Then expected records
      | topic_alias | key       | value       |
      | topic_out   | isUUID    | aliasValue1 |
      | topic_out   | isNotUUID | aliasValue2 |
    And assert aliasValue1 $ == "true"
    And assert aliasValue2 $ == "false"

  Scenario: sleep_now
    Given var date_before = call function : now
    And var print_first = call function : print sleep before ${date_before}
    When var sleep = call function : sleep 5000
    And var date_after = call function : now
    And var print_second = call function : print sleep after ${date_after}

  Scenario: uppercase
    Given var foo = bar
    And var uppercaseFoo = call function : uppercase ${foo}
    When records with key and value are sent
      | topic_alias | key       | value           |
      | topic_in    | UPPERCASE | ${uppercaseFoo} |
    Then expected records
      | topic_alias | key       | value       |
      | topic_out   | UPPERCASE | aliasValue1 |
    And assert aliasValue1 $ == "BAR"

  Scenario: lowercase
    Given var foo = BAR
    And var uppercaseFoo = call function : lowercase ${foo}
    When records with key and value are sent
      | topic_alias | key       | value           |
      | topic_in    | lowercase | ${uppercaseFoo} |
    Then expected records
      | topic_alias | key       | value       |
      | topic_out   | lowercase | aliasValue1 |
    And assert aliasValue1 $ == "bar"

  Scenario: sha256
    Given var foo = some description to hash
    And var sha256Foo = call function : sha256 ${foo}
    When records with key and value are sent
      | topic_alias | key    | value        |
      | topic_in    | sha256 | ${sha256Foo} |
    Then expected records
      | topic_alias | key    | value       |
      | topic_out   | sha256 | aliasValue1 |
    And assert aliasValue1 $ == "c4503e8f44c69fea01bca0a28acd5ca9f82d31dd287c9200729d7b11f5658be5"

  Scenario: sha1
    Given var foo = some description to hash
    And var sha1Foo = call function : sha1 ${foo}
    When records with key and value are sent
      | topic_alias | key  | value      |
      | topic_in    | sha1 | ${sha1Foo} |
    Then expected records
      | topic_alias | key  | value       |
      | topic_out   | sha1 | aliasValue1 |
    And assert aliasValue1 $ == "398ccd6616fb1ec2086eddfccea671823b58f466"

  Scenario: sha1_Interpolation
    Given var foo = some description to hash
    And var sha1Foo = call function : sha1 ${foo}bar
    When records with key and value are sent
      | topic_alias | key       | value      |
      | topic_in    | sha1Inter | ${sha1Foo} |
    Then expected records
      | topic_alias | key       | value       |
      | topic_out   | sha1Inter | aliasValue1 |
    And assert aliasValue1 $ == "aee512dd2d5d0f3ec26df2b2e0d583fe81a88ab4"
