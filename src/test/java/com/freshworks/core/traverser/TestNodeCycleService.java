package com.freshworks.core.traverser;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.UUID;

@SpringBootTest()
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestNodeCycleService {

    @Autowired
    ApplicationContext applicationContext;

}
