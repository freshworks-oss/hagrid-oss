package com.freshworks.core.runners;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({
        "com.freshworks.core.concurrency"
})
public class ConcurrencySuiteRunner {
}
