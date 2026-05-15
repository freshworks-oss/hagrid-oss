<!-- Steps START -->
<!-- Basics START -->

```java
@Slf4j
@FreshHierarchy(parentClass = ParentStep.class, rateLimit = 100, duration = 10)
@Component
@Scope("prototype")
public class FbUser extends HttpAbstractStep {
    
    // Override some methods here 
}
```

Above `FbUser` class (In Hagrid world, we call it `step`) will represent the `FbUser` DagNode. Take a note of annotation `FreshHiearchy` which tell the `level` of this node in the
DAG that Hagrid will create internally. If the `step` is top level `node` in the `DAG` then assign it `ParentStep` as parent step . 

Now lets take a look at `FbPost` class

```java
@Slf4j
@FreshHierarchy(parentClass = FbUser.class, rateLimit = 200, duration = 30, ignore = false)
@Component
@Scope("prototype")
public class FbPost extends HttpAbstractStep {

    // Override some methods here
}
```

Above `FbPost` step will present the `FbPost` DagNode. Take a note of annotation `FreshHierarchy` which has `FbUser.class` as parent.
By this annotation, `Hagrid` will internally know that it has to call `FbPost` for each `FbUser` or `FbPost` is the Child of `FbUser`.

```java
@Slf4j
@FreshHierarchy(parentClass = FbPost.class, rateLimit = 800, duration = 120, ignore = false)
@Component
@Scope("prototype")
public class FbComment extends HttpAbstractStep {

    // Override some methods here
}
```

```java
@Slf4j
@FreshHierarchy(parentClass = FbUser.class, rateLimit = 800, duration = 1, ignore = false)
@Component
@Scope("prototype")
public class FbCommunity extends HttpAbstractStep {

    // Override some methods here
}
```

Internally, Hagrid makes sure that it fetches `fbPost` for each `fbuser` and `fbComment` for each `fbPost`.
<!-- Basics END -->


<!-- List of Methods START -->

# configure 

Configure is the method that will be called once the object of `Step` is created. Parameter of `configure` method is `SyncServiceContainer`. 
You can think of `SyncServiceContainer` like `ApplicationContext` of spring. 
With `SyncServiceContainer` contains instances of `all Hagrid based services` which are relevant to the current run.

```java
@Override
    public void configure(SyncServiceContainer syncServiceContainer){
        // Here use syncService container to get InfraService, AnalyticsService, SyncStatusService etc. 
    }
```

# ShouldProceedWithParentObjectHttp

```java
@Override
    public Optional<Boolean> shouldProceedWithParentObject(JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("METHOD_CALLED", "name", "shouldProceedWithParentObject");
        return Optional.fromNullable(true);
    }
    
```
This method ask the dev whether `Hagrid` should move ahead for this `parent` object. Take an example of our `facebook connector`, 
in that assume we are at a step `FbComment` then `Hagrid` will pass all `parent`, `grandParent`, `greatGrandParent` data for which this step is getting executed.
So for `facebook based connector`, `parentJsonObject[0]` will contains `post` data , `parentJsonObject[1]` will contain `user` data and so on.  

Use this method to return `false` when you do not want to execute this step for particular kind of parent like `test-user` parents

# Setup 

```java
@Override
    public void setup(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("METHOD_CALLED", "name", "setup");
    }
    
```

This method is like a `pre-hook`, `Hagrid` called this method to allow dev to perform any kind of `logging`, `analytics` before it actually starts 
executing this method. 


# StartSync 

```java
@Override
    public Optional startSync(JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("METHOD_CALLED", "name", "startSync");
        try{
            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.initGet("https://l3rtckyana.execute-api.us-east-1.amazonaws.com/performance-testing/user?how_many=" + numberOfUsersEachPage);
            httpRequestResponse.setRequest(httpRequest);
            return Optional.fromNullable(httpRequestResponse);
        }
        catch (Exception e){
            e.printStackTrace();
            return Optional.absent();
        }
    }
```

This method, will be called to get the `first-url` to call to get the data for this step. Here you create the object of 
`HttpRequestResponse` and set the `HttpRequest` object. Hagrid will make a call to third-party and set the `HttpResponse` 
in the same `HttpRequestResponse`



# IsValidResponse

```java
@Override
    public Boolean isValidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {

        analyticsService.infoEvent("METHOD_CALLED", "name", "isValidResponse");
        if(currentRequest.getResponse().getCode() == 200){
            return true;
        }
        else{
            return false;
        }
    }
```


Once Hagrid fetches the data, it asked `dev` whether this response is valid or not. Here **`InValid response`** means that you want change the flow 
of the Hagrid. It is like `catch` for error handling. 

Return `false` i.e. `invalid` response in these cases

1. `Re-try with new request` 
2. `Hold and Re-try after sometime`
3. `Abort current parent and Continue with Next parent`

## Scenario Based Decisions

Whether response is `valid` or `invalid` is totally based on the scenario. So far I have seen two scenarios 


### Orchestration based connectors
In some scenarios like `developing orchestration based connectors` i.e connectors which executes actions on `third-party`, 
even if `third-party` returns `non 200 response` like `404 not found`, `dev` may not want to consider it as `invalid` response 
and want to consume the response so that it can further be passed to the client which executed this action. 


### Discovery based connectors
In other scenarios like `developing discovery based connectors` i.e connectors which discovery resources from third-party, 
`dev` may want to re-try after sometime, `retry` with new request. In all these cases, this method should return `false`. 



# handleInvalidResponse
Once `dev` return `false` from the `step` method `isValidResponse` then `Hagrid` will execute this method.

```java
@Override
public DagTraversalService.TraverseAction handleInvalidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws URISyntaxException, StepFailedException {
    
    // Return relevant Traverser action object here. 
}

```

## Retry With New Request
Take this action, when you want Hagrid to re-try with new request. It is mostly the case when say `auth` token has expired
and you would like to create `new request` with `new auth token`. Please note that in this case, `Hagrid` will resume the flow
as it is, just with `new request` that dev will pass. Here is the snippet for the same. 


```java
@Override
    public DagTraversalService.TraverseAction handleInvalidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws URISyntaxException, StepFailedException {
        HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setURI("some url here");
        httpRequest.setHeader("auth", "new auth header");
        DagTraversalService.TraverseAction traverseAction = new DagTraversalService.TraverseAction();
        traverseAction.retryWithNewRequest(httpRequestResponse);
        return null;
    }
```

## Hold and Retry after Some time

Take this action, when you want to take action after `X` amount of time. It may be because server is `exhausted`, `server is temporary` down etc. 

```java

@Override
    public DagTraversalService.TraverseAction handleInvalidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws URISyntaxException, StepFailedException {
        
        DagTraversalService.TraverseAction traverseAction = new DagTraversalService.TraverseAction();
        traverseAction.holdAndReTry(1, TimeUnit.SECONDS);
        return null;
    }
    
```

## Abort current parent

Take this action, when you want to skip this parent and start with new parent. To understand this better, take the example of 
`facebook` connector. Assume that you are in  the `fbPost` step and you hit some issue when fetching posts for the user 
with userId `test-user-id` then you want to `skip` current parent and want to continue with rest of the `users` fetched 
so far. 


```java
@Override
    public DagTraversalService.TraverseAction handleInvalidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws URISyntaxException, StepFailedException {

        DagTraversalService.TraverseAction traverseAction = new DagTraversalService.TraverseAction();
        traverseAction.abortCurrentParentAndContinueWithNextParentInstance();
        return null;
    }
```

# FilterResponse 
```java
@Override
    public void filterResponse(StepDataBeanMapping stepDataBeanMapping, JsonNode... parentJsonObject) throws StepFailedException {
        
    JsonNode data = stepDataBeanMapping.getJsonData();
    data.remove
    }
    
```

# ParseSyncResponse

```java
@Override
public StepDataBeanMapping parseSyncResponse(HttpRequestResponse httpRequestResponse, JsonNode... parentJsonObject) {
    
    try{
        ObjectMapper objectMapper = new ObjectMapper();

        StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
        String response = httpRequestResponse.getResponse().getBody();

        JsonNode jsonNode = objectMapper.readTree(response);
        stepDataBeanMapping.setParseSyncedResponseData(jsonNode.get("data").get("users"));
        stepDataBeanMapping.setBeanClass(com.freshworks.hagrid.beans.User.class);
        return stepDataBeanMapping;
    }
    catch (Exception e){
        e.printStackTrace();
        return null;
    }
}
```

Suppose data fetched from third-party is 

{%
include-markdown "partials/common.md"
start="<-- THIRD PARTY RESPONSE START -->"
end="<-- THIRD PARTY RESPONSE END -->"

%}


In the above method, you get `httpRequestResponse` which will contains the `response` from third-party.
This method expects return of object `StepBeanDataMapping` which contains the `JsonNode` and `Bean` class.

Object of `StepBeanDataMapping` as name implies define, `this data Of JsonNode type` should be deserialized into type of `this type of bean`

So, when creating this object `StepBeanDataMapping`, it is important for dev to extract the part of the response where actual data lies.
For instance in above example, check for

```java
JsonNode jsonNode = objectMapper.readTree(response);
stepDataBeanMapping.setParseSyncedResponseData(jsonNode.get("data").get("users"));
stepDataBeanMapping.setBeanClass(com.freshworks.hagrid.beans.User.class);
        
```

Here, dev is extracting data in `{data:{"users":[{u1},{u2},{u3}]}` explicitly. Then adding bean of type `User.class`.

Hagrid will internally de serialized the `JsonNode` into array of beans if `JsonNode` is of `JsonArray` type otherwise
into single bean if `JsonNode` is of `JsonObject` type.


# isSyncComplete

```java
@Override
    public Optional<Boolean> isSyncComplete(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("METHOD_CALLED", "name", "isSyncComplete");

        if(count < numberOfPagination){
            return Optional.fromNullable(false);
        }
        else{
            return Optional.fromNullable(true);
        }
    }
```


Once the `parseSyncMethod` is called, `Hagrid` asks `dev` that `isSyncComplete` i.e. could there be any more data for this `step`, 
return `true` if yes otherwise `return false`. This method helps to handle the `pagination` use cases. 


# getNextSyncRequest

```java
@Override
    public Optional getNextSyncRequest(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        try{
            analyticsService.infoEvent("METHOD_CALLED", "name", "getNextSyncRequest");
            analyticsService.infoEvent("THIRD_PARTY_API_CALLED");

            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.initGet("https://l3rtckyana.execute-api.us-east-1.amazonaws.com/performance-testing/user?has_next=true&how_many=100");
            httpRequestResponse.setRequest(httpRequest);

            count = count + 1;
//            Thread.sleep(waitBetweenPaginationInMs);
            return Optional.fromNullable(httpRequestResponse);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }
```

This method will only be executed when `isSyncComplete` returns `false`. In this method `dev` needs to return the next `URL` 
from where data has to be fetched. 

Important point to note here is that, `Hagrid` inject two important params so that `dev` can make the next `url`
1. Response from the last executed command. In case of http step, it is `httpRequestResponse`
2. Data of all its `parent`, `grandparent`, `great grandparent` and so on nodes. 


Once `hagrid` get the next URL to execute then it executes it and call the `isValidResponse` method again and so on. 


# closeSync
Once `isSyncComplete` method returns `true` then `Hagrid` calls this method as a `after hook`. Here `dev` can fire events, 
log something etc. 

```java
@Override
    public void closeSync() {
        analyticsService.infoEvent("METHOD_CALLED", "name", "closeSync");
    }
```

<!-- List of Methods END -->


<!-- Steps Start -->