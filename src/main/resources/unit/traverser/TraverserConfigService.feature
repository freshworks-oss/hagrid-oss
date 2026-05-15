Feature: Traverse Configuration Service

  Scenario: Get Traverser Thread Count
    Given the traverser configuration file "src/test/resources/hagrid.yaml" exists
    When the traverser thread count is retrieved
    Then the traverser thread count should be "1"

  Scenario: Get Step Location
    Given the traverser configuration file "src/test/resources/hagrid.yaml" exists
    When the traverser step location is retrieved
    Then the traverser step location should be "com.freshworks.steps"

  Scenario: Get Bean Location
    Given the traverser configuration file "src/test/resources/hagrid.yaml" exists
    When the traverser bean location is retrieved
    Then the traverser bean location should be "com.freshworks.beans"