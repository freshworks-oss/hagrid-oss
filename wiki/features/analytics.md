# Analytics Service

**Compatibility Matrix**

- [x] 3.1.0
- [ ] 3.0.0
- [ ] 2.0.0-beta
- [ ] 1.2.0

Analytics service is the service which enables developers to
1. Log structure events using @Slf4j 
2. Use Prometheus for counter and gauge
3. Register callbacks and react to the events fired anywhere in your application

### Summary of Analytics Service

#### Structure logging of events 
1. `analyticService.logDebugEvent("DEBUG_EVENT_NAME", "tag1", "value1", "tag2", "value2", "tag3", "value3")`
2. `analyticService.logInfoEvent("INFO_EVENT_NAME", "tag1", "value1", "tag2", "value2", "tag3", "value3")`
3. `analyticService.logWarnEvent("WARN_EVENT_NAME", "tag1", "value1", "tag2", "value2", "tag3", "value3")`
4. `analyticService.logErrorEvent("WARN_EVENT_NAME", "tag1", "value1", "tag2", "value2", "tag3", "value3")`

** application.log ** will look like this 
1. `event` will be mapped to `message` of the log file 
2. `tags and values` will be injected into json log
3. `debug or info or warn or error` will be injected into LEVEL field. 

```json
{"@timestamp":"2025-04-03T16:31:24.854881+05:30","@version":"1","message":"THIRD_PARTY_API_CALLED","logger_name":"com.freshworks.core.shared.analytics.AnalyticsService","thread_name":"DagNodePerItemTraversal_com.freshworks.hagrid.steps.Comment_com.freshworks.hagrid.steps.Comment","level":"INFO","level_value":20000,"traceId":"67ee6a824ff42a415ea609c8c53d7331","spanId":"d8cf8741f98e3cf9","caller":"com.freshworks.hagrid.steps.Comment.startSync#71","namespace":"ea51da9b-021a-449a-a98d-1af4ff225a46"}
{"@timestamp":"2025-04-03T16:31:24.854918+05:30","@version":"1","message":"METHOD_CALLED","logger_name":"com.freshworks.core.shared.analytics.AnalyticsService","thread_name":"DagNodePerItemTraversal_com.freshworks.hagrid.steps.Comment_com.freshworks.hagrid.steps.Comment","level":"INFO","level_value":20000,"traceId":"67ee6a824ff42a415ea609c8c53d7331","spanId":"d8cf8741f98e3cf9","caller":"com.freshworks.hagrid.steps.Comment.shouldProceedWithParentObject#51","name":"shouldProceedWithParentObject","namespace":"ea51da9b-021a-449a-a98d-1af4ff225a46"}
{"@timestamp":"2025-04-03T16:31:24.854969+05:30","@version":"1","message":"THIRD_PARTY_API_CALLED","logger_name":"com.freshworks.core.shared.analytics.AnalyticsService","thread_name":"DagNodePerItemTraversal_com.freshworks.hagrid.steps.Comment_com.freshworks.hagrid.steps.Comment","level":"INFO","level_value":20000,"traceId":"67ee6a824ff42a415ea609c8c53d7331","spanId":"d8cf8741f98e3cf9","caller":"com.freshworks.hagrid.steps.Comment.startSync#71","namespace":"ea51da9b-021a-449a-a98d-1af4ff225a46"}
{"@timestamp":"2025-04-03T16:31:24.855022+05:30","@version":"1","message":"METHOD_CALLED","logger_name":"com.freshworks.core.shared.analytics.AnalyticsService","thread_name":"DagNodePerItemTraversal_com.freshworks.hagrid.steps.Comment_com.freshworks.hagrid.steps.Comment","level":"INFO","level_value":20000,"traceId":"67ee6a824ff42a415ea609c8c53d7331","spanId":"d8cf8741f98e3cf9","caller":"com.freshworks.hagrid.steps.Comment.setup#46","name":"setup","namespace":"ea51da9b-021a-449a-a98d-1af4ff225a46"}
{"@timestamp":"2025-04-03T16:31:24.855127+05:30","@version":"1","message":"METHOD_CALLED","logger_name":"com.freshworks.core.shared.analytics.AnalyticsService","thread_name":"DagNodePerItemTraversal_com.freshworks.hagrid.steps.Comment_com.freshworks.hagrid.steps.Comment","level":"INFO","level_value":20000,"traceId":"67ee6a824ff42a415ea609c8c53d7331","spanId":"d8cf8741f98e3cf9","caller":"com.freshworks.hagrid.steps.Comment.startSync#57","name":"startSync","namespace":"ea51da9b-021a-449a-a98d-1af4ff225a46"}
{"@timestamp":"2025-04-03T16:31:24.855203+05:30","@version":"1","message":"THIRD_PARTY_API_CALLED","logger_name":"com.freshworks.core.shared.analytics.AnalyticsService","thread_name":"DagNodePerItemTraversal_com.freshworks.hagrid.steps.Comment_com.freshworks.hagrid.steps.Comment","level":"INFO","level_value":20000,"traceId":"67ee6a824ff42a415ea609c8c53d7331","spanId":"d8cf8741f98e3cf9","caller":"com.freshworks.hagrid.steps.Comment.startSync#71","namespace":"ea51da9b-021a-449a-a98d-1af4ff225a46"}
{"@timestamp":"2025-04-03T16:31:24.855329+05:30","@version":"1","message":"METHOD_CALLED","logger_name":"com.freshworks.core.shared.analytics.AnalyticsService","thread_name":"DagNodePerItemTraversal_com.freshworks.hagrid.steps.Comment_com.freshworks.hagrid.steps.Comment","level":"INFO","level_value":20000,"traceId":"67ee6a824ff42a415ea609c8c53d7331","spanId":"d8cf8741f98e3cf9","caller":"com.freshworks.hagrid.steps.Comment.shouldProceedWithParentObject#51","name":"shouldProceedWithParentObject","namespace":"ea51da9b-021a-449a-a98d-1af4ff225a46"}
```



As this log format is `logstash` compatible, you can inject these into `ELK` and run the visualisation graphs. 


#### Integration with Prometheus 
1. analyticsService.meterCounter("EVENT_NAME", "tag1", "value1", "tag2", "value2")
2. analyticsService.meterGauge("EVENT_NAME", value)


#### Firing Simple Events 
1. analyticsService.simpleEvent("EVENT_NAME", "tag1", "value1", "tag2", "value2")
2. If you use simple events, then these events will neither be logged in logs or prometheus. Mostly, you will use it when you want to trigger services register for this event 

#### Register Callbacks which reacts to events 
You can register a callback which will react to the EVENTS fired from anywhere in your application like below 


```java
analyticsService.registerEventCallback("SOME_IMPORTANT_EVENT", (eventTagAndValuePayload) -> {
    
        });
```
Callbacks will be triggered for event name matching the registered callback event name

**Note:**
Remember all events which are fired using `infoEvent`, `warnEvent` and `errorEvent`, `analyticsService` take the `event name` and register it with `prometheus`.
We do not register tags in prometheus by default because dev might give high cardinality tags which can cause high volume of time series on prometheus side.
If you want analytics service to log these tags as well then in `application.properties` using this value `spring.connector.analytics.meter.consume.event.tags`
We do not log event or its tags for debugEvent in any case.


## Use Analytics Service to Trigger Callbacks 

You can use `analyticsService` to make your application react to `events` raised in other part of the application. `Hagrid` uses analytics service 
for listening events from other services and react accordingly. 

Suppose you have 
1. Asset Object like `FbUser` 
2. Kafka Consumer like `kafkaConsumerService`

You want to consume assets as soon as it is generated and send it to kafka for further processing

```java


public class FbUser extends AbstractAsset {

    String userId;
    String userName;
    AnalyticsService analyticsService;

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) {
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        Namespace namespace = syncServiceContainer.getBean(Namespace.class);
        analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());

    }

    public void setBatchFromBean(com.freshworks.hagrid.beans.User dummy) {

        userId = dummy.getUser_id();
        userName = dummy.getUser_name();
    }


    @Override
    public void transform() {

        analyticsService.meterCounter("FB_ASSET_CREATED", "user_id", userId, "user_name", userName);
    }
}



// Somewhere else in your code kafkaConsumerService

// Register the callback to be called when particular event is fired. 
analyticsSerice.registerEventCallback("FB_ASSET_CREATED", (tags) ->{
            
            kafkaConsumer.send(tags);
            
        });


```















