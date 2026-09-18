package com.freshworks.core.integration.processor.test;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.integration\\..*")
public class TestProcessor {
}
