package com.freshworks.hagrid.runners;

import org.junit.platform.suite.api.*;

@Suite
@SelectPackages({
        "com.freshworks.hagridtraverser",
        "com.freshworks.hagridshared",
        "com.freshworks.hagridprocessor",
        // "com.freshworks.hagridshared.infra.inmemory"
})
public class UnitSuiteRunner {

}
