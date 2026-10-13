package com.freshworks.hagrid.runners;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;

@Suite
@SelectPackages({
        "com.freshworks.hagridintegration.traverser.test",
        "com.freshworks.hagridintegration.sync.test"
})
public class IntegrationSuiteRunner {
}
