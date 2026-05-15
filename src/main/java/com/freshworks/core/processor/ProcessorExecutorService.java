package com.freshworks.core.processor;


import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.synchronizers.GlobalNamespaceService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextSnapshotFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class ProcessorExecutorService {

    ExecutorService executorService;
    ExecutorService contextExecutorService ;
    AnalyticsService analyticsService;
    String globalNamespace;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    Multimap<String, Future<?>> taskManagerByNamespace = ArrayListMultimap.create();
    HashMap<String, Boolean> shutdownFlagManager = new HashMap<>();

    public ProcessorExecutorService(AnalyticsFactory analyticsFactory, GlobalNamespaceService globalNamespaceService){
        this.globalNamespace = globalNamespaceService.getGlobalNamespace();
        this.analyticsService = analyticsFactory.getAnalyticsService(globalNamespace);
        ThreadFactory factory = java.lang.Thread.ofPlatform().name("processor_task_", 1).factory();
        executorService =  Executors.newCachedThreadPool(factory);
        this.contextExecutorService = ContextExecutorService.wrap(executorService, ContextSnapshotFactory.builder().build()::captureAll);

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) this.executorService;
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getPoolSize, "service", "processor", "property", "pool_size");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getActiveCount, "service", "processor", "property", "active_count");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getTaskCount, "service", "processor","property", "task_count");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getMaximumPoolSize, "service", "processor", "property", "maximum_pool_size");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getLargestPoolSize,  "service", "processor", "property", "largest_pool_size");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getCompletedTaskCount, "service", "processor", "property", "completed_task_count");
    }

    public <T> Future<T> submit(String namespace, Callable<T> callableTask){

        try{
            lock.writeLock().lock();
            if(Boolean.FALSE.equals(shutdownFlagManager.containsKey(namespace))){
                Future<T> f = this.contextExecutorService.submit(callableTask);
                taskManagerByNamespace.put(namespace, f);
                return f;
            }

            else{
                throw new IllegalStateException("Processor submitting task while shutdown has been called for namespace = " + namespace);
            }
        }

        finally {
            lock.writeLock().unlock();
        }
    }

    public boolean interruptSync(String namespace){
        try{
            lock.writeLock().lock();
            shutdownFlagManager.put(namespace, true);
            Collection<Future<?>> futures = taskManagerByNamespace.get(namespace);
            for(Future<?> future : futures){
                future.cancel(true);
            }
        }

        catch (Exception e){

            // It is important to re-interrupt the thread so that it can be managed by traverser services.
            Thread.currentThread().interrupt();
        }

        finally {
            lock.writeLock().unlock();
        }

        return true;
    }

    public void destroy(String namespace){
        taskManagerByNamespace.removeAll(namespace);
        shutdownFlagManager.remove(namespace);
    }
}
