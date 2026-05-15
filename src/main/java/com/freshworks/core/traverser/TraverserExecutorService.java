package com.freshworks.core.traverser;

import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.synchronizers.GlobalNamespaceService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextSnapshotFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Component
public class TraverserExecutorService{

    ExecutorService executorService;
    ExecutorService contextExecutorService ;
    AnalyticsService analyticsService;
    String globalNamespace;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    Multimap<String, Future<?>> taskManagerByNamespace = ArrayListMultimap.create();
    HashMap<String, Boolean> shutdownFlagManager = new HashMap<>();

    @Autowired
    public TraverserExecutorService(AnalyticsFactory analyticsFactory, GlobalNamespaceService globalNamespaceService){
        this.globalNamespace = globalNamespaceService.getGlobalNamespace();
        this.analyticsService = analyticsFactory.getAnalyticsService(globalNamespace);
        ThreadFactory factory = java.lang.Thread.ofPlatform().name("traverser_executor_", 1).factory();
        executorService =  Executors.newCachedThreadPool(factory);
        this.contextExecutorService = ContextExecutorService.wrap(executorService, ContextSnapshotFactory.builder().build()::captureAll);

        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) this.executorService;
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getPoolSize, "service", "traverser", "property", "pool_size");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getActiveCount, "service", "traverser", "property", "active_count");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getTaskCount, "service", "traverser","property", "task_count");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getMaximumPoolSize, "service", "traverser", "property", "maximum_pool_size");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getLargestPoolSize,  "service", "traverser", "property", "largest_pool_size");
        analyticsService.meterGauge("HAGRID_EXECUTOR", threadPoolExecutor, ThreadPoolExecutor::getCompletedTaskCount, "service", "traverser", "property", "completed_task_count");
    }


    public <T> Future<T> submit(String namespace, Callable<T> callableTask) throws IllegalStateException{

        try{
            lock.writeLock().lock();
            if(Boolean.FALSE.equals(shutdownFlagManager.containsKey(namespace))){
                Future<T> f = this.contextExecutorService.submit(callableTask);
                taskManagerByNamespace.put(namespace, f);
                return f;
            }

            else{
                throw new IllegalStateException("traverser submitting task while shutdown has been called for namespace = " + namespace);
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

