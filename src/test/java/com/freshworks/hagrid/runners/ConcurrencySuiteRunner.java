package com.freshworks.hagrid.runners;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({
        "com.freshworks.hagridconcurrency"
})
public class ConcurrencySuiteRunner {
}
