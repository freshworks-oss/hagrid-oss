package com.freshworks.core.runners;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        UnitSuiteRunner.class,
        IntegrationSuiteRunner.class,
        DurabilitySuiteRunner.class,
        ConcurrencySuiteRunner.class,
})
public class MasterTestSuiteRunner {
}
