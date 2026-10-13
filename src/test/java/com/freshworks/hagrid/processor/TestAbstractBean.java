package com.freshworks.hagrid.processor;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAbstractBean {

    @Test
    void equalBeansWithDifferentParentsHaveConsistentHashes() {
        TestBean first = new TestBean("child");
        first.parentBean = new TestBean("first-parent");
        TestBean second = new TestBean("child");
        second.parentBean = new TestBean("second-parent");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());

        Set<AbstractBean> beans = new HashSet<>();
        beans.add(first);
        assertTrue(beans.contains(second));
        assertFalse(beans.add(second));
        assertEquals(1, beans.size());
    }

    public static class TestBean extends AbstractBean {
        public String value;

        public TestBean(String value) {
            this.value = value;
        }

        @Override
        public void transform() {
        }
    }
}
