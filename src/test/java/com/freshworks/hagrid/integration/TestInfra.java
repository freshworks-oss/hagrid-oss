package com.freshworks.hagrid.integration;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.integration\\..*")
public class TestInfra {
}
