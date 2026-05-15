# Beans Use cases

## Access Hagrid Internal Services
In each bean, a developer can access Hagrid's internal services via service `SyncContainerService`. Each step inherits a method called
`getSyncContainerService()` from parent class `AbstractBean`.

Using method `getSyncContainerService()`, developer can access the sync container. Once developer has the syncContainerService, any internal service
can be accessed like this

```java
SyncContainerService syncContainerService = getSyncContainerService();
SyncService syncService = syncContainerService.getBean(SyncService.class);
AnalyticsFactory analyticsFactory = syncContainerService.getBean(AnalyticsFactory.class);
Namespace namespace = syncServiceContainer.getBean(Namespace.class);
AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
```

## Add, Update or Delete Attributes

Suppose, you want to add , update or delete attributes of the bean, then it can be done via `transform` method that is 
inherited from parent class `AbstractBean`

```java
import java.util.Date;

// Add attributes
@Override
public void transform() {

    this.created_at = new Date();
}

// Modify attributes
@Override
public void transform() {

    this.name = this.name.toLower();
}


// Delete unwanted attributes
@Override
public void transform() {

    this.unwantedAttribute = null;
}

```

## Filter Beans 

You can also filter beans which does not meet the business requirements. For this `Hagrid` provides a method `filter` which 
is inherited from the class `AbstractBean`

```java
@Override
public void filter() {

    if(this.email == null){
        return false;
    }
}

```

## Split Bean into multiple Beans 

There are the cases wherein you would like to split the bean into multiple smaller beans. For instance, 
suppose you got a bean like this 

```json
{
  "name" : "Amit Aggarwal",
  "company" : "freshworks",
  "address_lines" : [{
    "line 1" : "Bangalore"
  },{
    "line 1" : "Chennai" 
  }]
}
```

Now, you would like split into 

```json
{
  "name" : "Amit Aggarwal",
  "company" : "freshworks",
  "address" : {
    "line 1" : "Bangalore"
  }
}
```

```json
{
  "name" : "Amit Aggarwal",
  "company" : "freshworks",
  "address" : {
    "line 1" : "Chennai"
  }
}
```

for this purpose, use `map` method provided to achieve the same

```java
public List<AbstractBean> map() {
  
  // here split this bean into list of other beans 
    
}
```

## Get Parent Bean 

There will be the use case where you would like to add / update or delete some properties based on the parent bean. 
You can access the parentBean of `this` bean via method `getParent`. Suppose you are in `fbComment` bean below. 

```java

public void transform(){
    FbPost postBean = (FbPost)this.getParent();
    
    // This is the post for which this comment is being created
    String postId = postBean.getPostId();
}

```


