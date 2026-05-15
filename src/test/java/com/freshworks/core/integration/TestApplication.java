package com.freshworks.core.integration;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.integration\\..*")
public class TestApplication {
}
