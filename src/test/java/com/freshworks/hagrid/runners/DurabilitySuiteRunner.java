package com.freshworks.hagrid.runners;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({
        "com.freshworks.hagriddurability"
})
public class DurabilitySuiteRunner {
}
