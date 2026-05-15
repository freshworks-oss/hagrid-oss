package com.freshworks.core.runners;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;

@Suite
@SelectPackages({
        "com.freshworks.core.integration.traverser.test",
        "com.freshworks.core.integration.sync.test"
})
public class IntegrationSuiteRunner {
}
