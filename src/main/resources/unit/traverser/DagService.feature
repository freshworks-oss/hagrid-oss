Feature: Directed Acyclic Graph
  In this feature, Hagrid is suppose to create DAG from step classes presented in the particular directory.
  To build DAG, we can take the help of FreshHierarchy component which describes how different steps should
  be co-related

  Scenario: While creating DAG, if some steps are dynamically ignored then DAG service should ignore those steps irrespective
  of their statically typed ignore flag.
    Given Path "com.freshworks.core.data.dag.steps" where list of steps are present
    And Steps data is
      | Step                                                 | isIgnored | stepData|
      | com.freshworks.core.data.dag.steps.Application       | No |[{\"key\":\"1\"}]|
      | com.freshworks.core.data.dag.steps.ServicePrinciple  | No |[{\"key\":\"1\"}]|
      | com.freshworks.core.data.dag.steps.AppRoleAssignment | yes |[{\"key\":\"1\"}]|
      | com.freshworks.core.data.dag.steps.User              | No |[{\"key\":\"1\"}]|
      | com.freshworks.core.data.dag.steps.Group             | No |[{\"key\":\"1\"}]|
      | com.freshworks.core.data.dag.steps.Usage             | No |[{\"key\":\"1\"}]|


    When Steps are scanned to create the DAG
    Then DAG is created

    And Steps should be ignored
      |Step|
      |com.freshworks.core.data.dag.steps.AppRoleAssignment|

    And Steps should not be ignored
      |Step|
      |com.freshworks.core.data.dag.steps.Application     |
      |com.freshworks.core.data.dag.steps.ServicePrinciple|
      |com.freshworks.core.data.dag.steps.Usage           |



  Scenario: While creating DAG, if some steps are statically ignored then DAG service should ignore those steps.
    Given Path "com.freshworks.core.data.dag.steps" where list of steps are present
    And Steps data is
      | Step                                                 | isIgnored | stepData|
      | com.freshworks.core.data.dag.steps.Application       | No |[{\"key\":\"1\"}]|
      | com.freshworks.core.data.dag.steps.ServicePrinciple  | No |[{\"key\":\"1\"}]|
      | com.freshworks.core.data.dag.steps.AppRoleAssignment | No |[{\"key\":\"1\"}]|
      | com.freshworks.core.data.dag.steps.User              | No |[{\"key\":\"1\"}]|
      | com.freshworks.core.data.dag.steps.Group             | No |[{\"key\":\"1\"}]|
      | com.freshworks.core.data.dag.steps.Usage             | No |[{\"key\":\"1\"}]|
      | com.freshworks.core.data.dag.steps.Ignored           | No |[{\"key\":\"1\"}]|

    When Steps are scanned to create the DAG
    Then DAG is created
    And Steps should be ignored
      |Step|
      |com.freshworks.core.data.dag.steps.Ignored|

    And Steps should not be ignored
      |Step|
      |com.freshworks.core.data.dag.steps.Application        |
      |com.freshworks.core.data.dag.steps.ServicePrinciple   |
      |com.freshworks.core.data.dag.steps.AppRoleAssignment  |
      |com.freshworks.core.data.dag.steps.User               |
      |com.freshworks.core.data.dag.steps.Group              |
      |com.freshworks.core.data.dag.steps.Usage              |


  Scenario: When step path is wrong then DAG creation should fail.
    Given Path "com.freshworks.core.traverser.dag.data.xxxx" where list of steps are present
    When  Steps are scanned to create the DAG
    Then  DAG should have only parent step

  Scenario: Parent child relationship
    Given Path "com.freshworks.core.data.dag.steps" where list of steps are present
    When Steps are scanned to create the DAG
    Then DAG is created with right Hierarchy
      | parentStep                                           | childStep                                            |
      | com.freshworks.core.traverser.ParentStep             | com.freshworks.core.data.dag.steps.Application        |
      | com.freshworks.core.data.dag.steps.Application       | com.freshworks.core.data.dag.steps.ServicePrinciple  |
      | com.freshworks.core.data.dag.steps.Application       | com.freshworks.core.data.dag.steps.Usage             |
      | com.freshworks.core.data.dag.steps.ServicePrinciple  | com.freshworks.core.data.dag.steps.AppRoleAssignment |
      | com.freshworks.core.data.dag.steps.AppRoleAssignment | com.freshworks.core.data.dag.steps.User              |
      | com.freshworks.core.data.dag.steps.AppRoleAssignment | com.freshworks.core.data.dag.steps.Group             |
