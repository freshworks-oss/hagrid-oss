package com.freshworks.core.shared.analytics;

import com.freshworks.core.shared.Annotations.AlphaRelease;
import com.freshworks.core.shared.Annotations.BetaRelease;
import com.freshworks.core.shared.executor.SharedExecutorService;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;

import static net.logstash.logback.argument.StructuredArguments.entries;
import static net.logstash.logback.argument.StructuredArguments.f;


@Slf4j
@BetaRelease(sourceVersion = "3.0.0-beta", targetVersion = "3.1.0", useCase = "Provide structured logging framework along with metrics for hagrid")
public class AnalyticsService {

    String namespace;
    AtomicLong numberOfErrorEvents = new AtomicLong(0);
    AtomicLong numberOfWarningEvents = new AtomicLong(0);

    MeterRegistry meterRegistry;

    AnalyticsUtility analyticsUtility;
    HashMap<String, List<Consumer<Map<String, Object>>>> consumerHashMap = new HashMap<>();
    HashMap<String, String> registeredGauge = new HashMap<>();

    String NAMESPACE_KEY = "namespace";

    Boolean shouldPassTagsToMeterRegistry;


    protected AnalyticsService( MeterRegistry meterRegistry, AnalyticsUtility analyticsUtility) {
        this.meterRegistry = meterRegistry;
        this.analyticsUtility = analyticsUtility;
    }

    protected void configure(String namespace, Boolean shouldPassTagsToMeterRegistry){
        this.namespace = namespace;
        this.shouldPassTagsToMeterRegistry = shouldPassTagsToMeterRegistry;
    }

    public void debugEvent(String eventName, Object... tags){

        Preconditions.checkNotNull(namespace, "namespace can not be null. Please configure the analytics service by calling configure method one it");

        Map<String, Object> s = analyticsUtility.processTagListIntoMap(tags);
        s.put(NAMESPACE_KEY, namespace);
        addCallerInformation(s);
        log.debug(eventName, entries(s));

        // Here I am making a callback called if this event type is present
        if(consumerHashMap.containsKey(eventName)){
            consumerHashMap.get(eventName).forEach(consumer -> consumer.accept(s));
        }
    }

    public void infoEvent(String eventName, Object... tags){

        Preconditions.checkNotNull(namespace, "namespace can not be null. Please configure the analytics service by calling configure method one it");

        Map<String, Object> s = analyticsUtility.processTagListIntoMap(tags);
        s.put(NAMESPACE_KEY, namespace);
        addCallerInformation(s);
        log.info(eventName, entries(s));

        // Here I am firing event to meterRegistry
        fireMeter(eventName, tags);

        // Here I am making a callback called if this event type is present
        if(consumerHashMap.containsKey(eventName)){
            consumerHashMap.get(eventName).forEach(consumer -> consumer.accept(s));
        }
    }


    public void warnEvent(String eventName, Object... tags){

        Preconditions.checkNotNull(namespace, "namespace can not be null. Please configure the analytics service by calling configure method one it");

        Map<String, Object> s = analyticsUtility.processTagListIntoMap(tags);
        s.put(NAMESPACE_KEY, namespace);
        addCallerInformation(s);
        log.warn(eventName, entries(s));

        // Here I am firing event to meterRegistry
        fireMeter(eventName, tags);

        numberOfWarningEvents.incrementAndGet();

        // Here I am making a callback called if this event type is present
        if(consumerHashMap.containsKey(eventName)){
            consumerHashMap.get(eventName).forEach(consumer -> consumer.accept(s));
        }
    }

    public void errorEvent(String eventName, Object... tags){

        Preconditions.checkNotNull(namespace, "namespace can not be null. Please configure the analytics service by calling configure method one it");

        Map<String, Object> s = analyticsUtility.processTagListIntoMap(tags);
        s.put(NAMESPACE_KEY, namespace);
        addCallerInformation(s);
        log.error(eventName, entries(s));

        // Here I am firing event to meterRegistry
        fireMeter(eventName, tags);

        numberOfErrorEvents.incrementAndGet();

        // Here I am making a callback called if this event type is present
        if(consumerHashMap.containsKey(eventName)){
            consumerHashMap.get(eventName).forEach(consumer -> consumer.accept(s));
        }
    }

    /**
     * Use simpleEvent to fire event on which other services may be listening on. This event is not logged in any of logger or metrics
     * @param eventName
     * @param tags
     */
    public void simpleEvent(String eventName, Object... tags){

        Preconditions.checkNotNull(namespace, "namespace can not be null. Please configure the analytics service by calling configure method one it");

        Map<String, Object> s = analyticsUtility.processTagListIntoMap(tags);
        s.put(NAMESPACE_KEY, namespace);

        // Here I am making a callback called if this event type is present
        if(consumerHashMap.containsKey(eventName)){
            consumerHashMap.get(eventName).forEach(consumer -> consumer.accept(s));
        }
    }

    public void meterCounter(String eventName, Object... tags){

        Preconditions.checkNotNull(namespace, "namespace can not be null. Please configure the analytics service by calling configure method one it");

        String[] strings = new String[tags.length];

        for(int i = 0; i < tags.length; i++){
            strings[i] = String.valueOf(tags[i]);
        }
        if(strings.length > 0 ){
            this.meterRegistry.counter(eventName, strings).increment(1);
        }
        else{
            this.meterRegistry.counter(eventName).increment(1);
        }

    }

    public void meterCounterByIncrement(String eventName, int incrementBy, Object... tags){

        Preconditions.checkNotNull(namespace, "namespace can not be null. Please configure the analytics service by calling configure method one it");

        String[] strings = new String[tags.length];

        for(int i = 0; i < tags.length; i++){
            strings[i] = String.valueOf(tags[i]);
        }

        if(strings.length > 0 ){
            this.meterRegistry.counter(eventName, strings).increment(incrementBy);
        }
        else{
            this.meterRegistry.counter(eventName).increment(incrementBy);
        }


    }

    public <T> void meterGauge(String eventName, T thisObject, ToDoubleFunction<T> function, Object... tags){
        Preconditions.checkNotNull(namespace, "namespace can not be null. Please configure the analytics service by calling configure method one it");

        // Here I am making sure, gauges are registered only once.
        Preconditions.checkArgument(Boolean.FALSE.equals(registeredGauge.containsKey(eventName)), "Duplicate registration of Gauge. Micrometer Gauges MUST be registered only once");

        String[] strings = new String[tags.length];

        for(int i = 0; i < tags.length; i++){
            strings[i] = String.valueOf(tags[i]);
        }

        if(strings.length == 0 ){
            Gauge.builder(eventName, thisObject, function).register(this.meterRegistry);
        }

        else{

            Gauge.builder(eventName, thisObject, function)
                    .tags(strings)
                    .register(this.meterRegistry);
        }
    }


    public boolean anyWarnEvent(){

        double d =  numberOfWarningEvents.get();

        return d != 0;
    }

    public boolean anyErrorEvent(){

        double d = numberOfErrorEvents.get();
        return d != 0;
    }


    public double howManyWarnEvent(){

        return numberOfWarningEvents.get();
    }

    public double howManyErrorEvent(){

        return numberOfErrorEvents.get();
    }


    /**
     * Register a consumer which will be called whenever a event with given name is fired anywhere across application
     * @param eventName - name of the event for which callback should be fired
     * @param consumer - Lambda method which will be called. Event Tags values will be passed to it.
     */

    @AlphaRelease(sourceVersion = "3.0.0-beta", targetVersion = "3.1.0", useCase = "Dev can use this to trigger methods based on various events fired from different part of the application. For example - When dev fired an event from asset transform method then call this consumer to consme the asset")
    public void registerEventCallback(String eventName, Consumer<Map<String, Object>> consumer){
        if(consumerHashMap.containsKey(eventName)){
            List<Consumer<Map<String, Object>>> consumers = consumerHashMap.get(eventName);
            consumers.add(consumer);
        }
        else{
            List<Consumer<Map<String, Object>>> consumers = new ArrayList<>();
            consumers.add(consumer);
            consumerHashMap.put(eventName, consumers);
        }
    }


    private void fireMeter(String eventName, Object... s){


        String[] meterTagList = new String[s.length];

        if(shouldPassTagsToMeterRegistry && s.length %2 == 0){
            int i = 0;
            for(Object o : s){
                meterTagList[i] = String.valueOf(o);
                i = i + 1;
            }

            this.meterRegistry.counter(eventName, meterTagList).increment(1);
        }
        else{
            this.meterRegistry.counter(eventName).increment(1);
        }
    }

    private StackWalker.StackFrame getStackFrame(){

        StackWalker.StackFrame frame = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames.skip(2).findFirst().orElse(null));

        return frame;
    }


    private void addCallerInformation(Map<String, Object> s) {
        if (s != null) {
            StackWalker.StackFrame frame = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .walk(frames -> frames.skip(2).findFirst().orElse(null));

            if (frame != null) {
                s.put("caller", String.format("%s.%s#%d", frame.getClassName(), frame.getMethodName(), frame.getLineNumber()));
            }
        }
    }


}