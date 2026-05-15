# Created by aaggarwal at 13/08/24
Feature: Traverser
  # Enter feature description here

  Scenario: Test the methods of the steps are called in the expected order
    Given Dag created from steps present at "com.freshworks.core.data.traverser.single.steps"
    When Traverser traverse the Dag
    Then Methods of the step "com.freshworks.core.data.traverser.single.steps" are called as per step lifecycle