package com.freshworks.core.shared.executor;

import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.synchronizers.GlobalNamespaceService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class SharedExecutorService{

    ExecutorService executorService;
    ExecutorService contextExecutorService ;
    AnalyticsService analyticsService;
    String globalNamespace;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    Multimap<String, Future<?>> taskManagerByNamespace = ArrayListMultimap.create();
    HashMap<String, Boolean> shutdownFlagManager = new HashMap<>();

    public SharedExecutorService(AnalyticsFactory analyticsFactory, GlobalNamespaceService globalNamespaceService){
        this.globalNamespace = globalNamespaceService.getGlobalNamespace();
        this.analyticsService = analyticsFactory.getAnalyticsService(globalNamespace);
        ThreadFactory factory = java.lang.Thread.ofPlatform().name("shared_executor_", 1).factory();
        executorService =  Executors.newCachedThreadPool(factory);
        this.contextExecutorService = ContextExecutorService.wrap(executorService, ContextSnapshotFactory.builder().build()::captureAll);

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) this.executorService;
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getPoolSize, "service", "shared", "property", "pool_size");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getActiveCount, "service", "shared", "property", "active_count");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getTaskCount, "service", "shared","property", "task_count");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getMaximumPoolSize, "service", "shared", "property", "maximum_pool_size");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getLargestPoolSize,  "service", "shared", "property", "largest_pool_size");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getCompletedTaskCount, "service", "shared", "property", "completed_task_count");
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
                throw new IllegalStateException("Shared submitting task while shutdown has been called for namespace = " + namespace);
            }
        }

        finally {
            lock.writeLock().unlock();
        }
    }

    public boolean shutdownNow(String namespace){

        return shutdownNow(namespace, 10);
    }

    public boolean shutdownNow(String namespace, int waitInSeconds){
        try{
            lock.writeLock().lock();
            shutdownFlagManager.put(namespace, true);
            Collection<Future<?>> futures = taskManagerByNamespace.get(namespace);
            for(Future<?> future : futures){
                future.cancel(true);
            }

            TimeUnit.SECONDS.sleep(waitInSeconds);
        }
        catch (InterruptedException e){

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
