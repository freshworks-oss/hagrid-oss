package com.freshworks.core.shared.analytics;

import static net.logstash.logback.argument.StructuredArguments.entries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;

import com.freshworks.core.shared.Annotations.AlphaRelease;
import com.freshworks.core.shared.Annotations.BetaRelease;
import com.google.common.base.Preconditions;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@BetaRelease(sourceVersion = "3.0.0-beta", targetVersion = "3.1.0", useCase = "Provide structured logging framework along with metrics for hagrid")
public class AnalyticsService {

    String namespace;
    AtomicLong numberOfDebugEvents = new AtomicLong(0);
    AtomicLong numberOfInfoEvents = new AtomicLong(0);
    AtomicLong numberOfWarningEvents = new AtomicLong(0);
    AtomicLong numberOfErrorEvents = new AtomicLong(0);

    ConcurrentHashMap<String, AtomicLong> appEventsMap = new ConcurrentHashMap<>();

    MeterRegistry meterRegistry;

    AnalyticsUtility analyticsUtility;
    HashMap<String, List<Consumer<Map<String, Object>>>> consumerHashMap = new HashMap<>();
    HashMap<String, String> registeredGauge = new HashMap<>();

    String NAMESPACE_KEY = "namespace";

    boolean shouldPassLogEventTagsToMeterRegistry = false;
    boolean shouldPrintSummaryOnDestroy = true;


    protected AnalyticsService( MeterRegistry meterRegistry, AnalyticsUtility analyticsUtility) {
        this.meterRegistry = meterRegistry;
        this.analyticsUtility = analyticsUtility;
    }

    protected void configure(String namespace, Boolean shouldPassTagsToMeterRegistry){
        this.namespace = namespace;
        this.shouldPassLogEventTagsToMeterRegistry = shouldPassTagsToMeterRegistry;
    }

    /**
     * Use this method to log a event in log file
     * @param eventName
     * @param tags
     */
    public void debugLogEvent(String eventName, Object... tags){

        Preconditions.checkNotNull(namespace, "namespace can not be null. Please configure the analytics service by calling configure method one it");

        Map<String, Object> s = analyticsUtility.processTagListIntoMap(tags);
        s.put(NAMESPACE_KEY, namespace);
        addCallerInformation(s);
        log.debug(eventName, entries(s));

        // Here I am firing event to meterRegistry
        fireMeter(eventName, tags);

        numberOfDebugEvents.incrementAndGet();

        // Here I am making a callback called if this event type is present
        if(consumerHashMap.containsKey(eventName)){
            consumerHashMap.get(eventName).forEach(consumer -> consumer.accept(s));
        }
    }

    /**
     * Use this method to log a event in log file
     * @param eventName
     * @param tags
     */

    public void infoLogEvent(String eventName, Object... tags){

        Preconditions.checkNotNull(namespace, "namespace can not be null. Please configure the analytics service by calling configure method one it");

        Map<String, Object> s = analyticsUtility.processTagListIntoMap(tags);
        s.put(NAMESPACE_KEY, namespace);
        addCallerInformation(s);
        log.info(eventName, entries(s));

        // Here I am firing event to meterRegistry
        fireMeter(eventName, tags);

        numberOfInfoEvents.incrementAndGet();

        // Here I am making a callback called if this event type is present
        if(consumerHashMap.containsKey(eventName)){
            consumerHashMap.get(eventName).forEach(consumer -> consumer.accept(s));
        }
    }


    /**
     * Use this method to log a event in log file
     * @param eventName
     * @param tags
     */
    public void warnLogEvent(String eventName, Object... tags){

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

    /**
     * Use this method to log a event in log file
     * @param eventName
     * @param tags
     */
    public void errorLogEvent(String eventName, Object... tags){

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
     * Use app event method when you want to fire an event without logging it 
     * @param eventName
     * @param tags
     */
    public void appEvent(String eventName, Object... tags){

        Preconditions.checkNotNull(namespace, "namespace can not be null. Please configure the analytics service by calling configure method one it");

        Map<String, Object> s = analyticsUtility.processTagListIntoMap(tags);
        s.put(NAMESPACE_KEY, namespace);

        // Here I am firing event to meterRegistry
        fireMeter(eventName, tags);

        if(appEventsMap.contains(eventName)){
            AtomicLong count = appEventsMap.get(eventName);
            count.incrementAndGet();
        }
        else{
            appEventsMap.put(eventName, new AtomicLong(0));
        }

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


    public boolean anyDebugEvent(){

        double d =  numberOfDebugEvents.get();

        return d != 0;
    }

    public boolean anyInfoEvent(){

        double d =  numberOfInfoEvents.get();

        return d != 0;
    }

    public boolean anyWarnEvent(){

        double d =  numberOfWarningEvents.get();

        return d != 0;
    }

    public boolean anyErrorEvent(){

        double d = numberOfErrorEvents.get();
        return d != 0;
    }

    public boolean anyAppEvent(){

        double d = appEventsMap.keySet().size();
        return d != 0;
    }


    public double howManyDebugEvent(){

        return numberOfDebugEvents.get();
    }

    public double howManyInfoEvent(){

        return numberOfInfoEvents.get();
    }

    public double howManyWarnEvent(){

        return numberOfWarningEvents.get();
    }

    public double howManyErrorEvent(){

        return numberOfErrorEvents.get();
    }

    public double howManyAppEvent(){

        return appEventsMap.keySet().size();
    }


    public void destroy(){

        if (shouldPrintSummaryOnDestroy){

            System.out.println("Below is the analytics Report");

            String anyWarning = anyWarnEvent() ? "yes" : "false";
            System.out.println("Any Warning Event ? " + anyWarning);

            String anyError = anyErrorEvent() ? "yes" : "false";
            System.out.println("Any Error Event ? " + anyError);

            String anyInfo = anyInfoEvent() ? "yes" : "false";
            System.out.println("Any Info Event ? " + anyInfo);

            String anyDebug = anyDebugEvent() ? "yes" : "false";
            System.out.println("Any Debug Event ? " + anyDebug);

            String anyApp = anyAppEvent() ? "yes" : "false";
            System.out.println("Any App Event ? " + anyApp);


            System.out.println("No. warn events ? " + numberOfWarningEvents.get());

            System.out.println("No. error events ? " + numberOfErrorEvents.get());

            System.out.println("No. info events ? " + numberOfInfoEvents.get());

            System.out.println("No. Debug events ? " + numberOfDebugEvents.get());

            System.out.println("No. App events ? " + appEventsMap.keySet().size());

            System.out.println("List of App Events fired are below");
            for(Map.Entry<String, AtomicLong> entry: appEventsMap.entrySet()){
                System.out.println(entry.getKey() + " : " + entry.getValue());
            }
        }

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

        if(shouldPassLogEventTagsToMeterRegistry && s.length %2 == 0){
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