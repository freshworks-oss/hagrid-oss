package com.freshworks.core.shared.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

@Configuration
public class DeadlockMetricsConfig {

    private final MeterRegistry meterRegistry;

    public DeadlockMetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void registerDeadlockMetrics() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        // Register a custom gauge for deadlocked threads
        Gauge.builder("jvm.threads.deadlocked", threadMXBean, bean -> {
                    long[] deadlockedThreads = bean.findDeadlockedThreads();
                    return deadlockedThreads == null ? 0 : deadlockedThreads.length;
                }).description("Number of threads that are deadlocked")
                .register(meterRegistry);

        // Register a custom gauge for monitor deadlocked threads
        Gauge.builder("jvm.threads.deadlocked.monitor", threadMXBean, bean -> {
                    long[] monitorDeadlockedThreads = bean.findMonitorDeadlockedThreads();
                    return monitorDeadlockedThreads == null ? 0 : monitorDeadlockedThreads.length;
                }).description("Number of threads that are deadlocked waiting for object monitors")
                .register(meterRegistry);
    }
}

