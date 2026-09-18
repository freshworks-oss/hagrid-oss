package com.freshworks.core.runners;

import org.junit.platform.suite.api.*;

@Suite
@SelectPackages({
        "com.freshworks.core.traverser",
        "com.freshworks.core.shared",
        "com.freshworks.core.processor",
        // "com.freshworks.core.shared.infra.inmemory"
})
public class UnitSuiteRunner {

}
