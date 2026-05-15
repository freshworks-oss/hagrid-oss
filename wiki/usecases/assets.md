# Assets Use cases

## Access Hagrid Internal Services
In each bean, a developer can access Hagrid's internal services via service `SyncContainerService`. Each step inherits a method called
`getSyncContainerService()` from parent class `AbstractAsset`.

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

Suppose, you want to add , update or delete attributes of the assets, then it can be done via `transform` method that is
inherited from parent class `AbstractAsset`

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

## Filter Assets

You can also filter assets which does not meet the business requirements. For this `Hagrid` provides a method `filter` which
is inherited from the class `AbstactAsset`

```java
@Override
public void filter() {

    if(this.email == null){
        return false;
    }
}

```

## Join two Bean 


## Join three or more Bean






