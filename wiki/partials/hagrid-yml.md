<!-- HagridYAML START -->

Create `hagrid.yaml` in `resources` directory of your `maven` project. 

`Hagrid.yaml` looks like this 

```yaml

---
configuration:
  connector:
    ## Path of the package where your steps, beans and assets resides. 
    step_path: "com.freshworks.hagrid.steps" 
    bean_path: "com.freshworks.hagrid.beans"
    asset_path: "com.freshworks.hagrid.assets"

  core:
    publisher:
      driver: "com.freshworks.core.publisher.MongoDb"
      thread_count: 100
      poll_count: 100

    processor:
      
      ## How many messages processor should process at once. 
      poll_count: 1000
      
      ## How many processors should run parallely. 
      number_of_parallel_processor : 20


    infra:
      
      ## Hagrid supports three kinds of infra "nitrite", "persistent" and "immemory" 
      ## nitrite means, it will use nitrite file based database. Persistent means, it will use mongodb for storing data and inmemory means, it will use RAM for the same
      infra_type: "persistent"
      environment: "development"
      persistent:
        development:
          database:
            host: "localhost"
            port: "27017"
            username: "user name here"
            password: "some password here"
            authDb: "admin"

        staging:
          database:
            host: "10.197.215.216"
            port: "27017"
            username: "user name here"
            password: "some password here"
            authDb: "admin"

        production:
          database:
            host: "10.197.215.216"
            port: "27017"
            username: "user name here"
            password: "some password here"
            authDb: "admin"
```


# Step, Bean & Asset Path 

Value of

1. `connector.configuration.step_path`
2. `connector.configuration.bean_path` 
3. `connector.configuration.asset_path` 

should be the directory where these classes lie in. 


# Configure Processor 

As you may recall, `processor` is the service, which captures `beans` from `staging area` (more precisely from `processorQueue` )
, transform them into `assets` or join multiple beans to create a complex `assets`. 

With the configuration `processor.poll_count` you mentioned how many messages each processor can process at once. More messages
mean more memory consumption
With the configuration `processor.number_of_parallel_processor` you mention how many parallel processor service can run. More proceses
means more CPU is needed.


# Configure Infra 

Here `infra` means some kind of `persistent` layer which can be used by Hagrid to store data it fetched from third party. 
Out of the box, `hagrid` supports three kinds of `Infra`. 

Configuration `infra.infra_type` could be 

1. persistent - Here `Hagrid` expects `Mongodb` configuration to be provided so that it can save and retrieve data during sync.   
2. inmemory -  Here no extra configuration is needed.
3. nitrite - It is file based database to be used. 


## Examples 
Here are the `snippets` for three kind of infra and how to use 


### Peristent Infra 
It means that you want Hagrid to store its data that it fetches from third-party into `mongodb` . Its configuration will look like below 


```yaml

---
configuration:
  connector:
    ## Path of the package where your steps, beans and assets resides. 
    step_path: "com.freshworks.hagrid.steps" 
    bean_path: "com.freshworks.hagrid.beans"
    asset_path: "com.freshworks.hagrid.assets"

  core:
    publisher:
      driver: "com.freshworks.core.publisher.MongoDb"
      thread_count: 100
      poll_count: 100

    processor:
      
      ## How many messages processor should process at once. 
      poll_count: 1000
      
      ## How many processors should run parallely. 
      number_of_parallel_processor : 20


    infra:
      
      ## Hagrid supports three kinds of infra "nitrite", "persistent" and "immemory" 
      ## nitrite means, it will use nitrite file based database. Persistent means, it will use mongodb for storing data and inmemory means, it will use RAM for the same
      infra_type: "persistent"
      environment: "development"
      persistent:
        development:
          database:
            host: "localhost"
            port: "27017"
            username: "user name here"
            password: "some password here"
            authDb: "admin"

        staging:
          database:
            host: "10.197.215.216"
            port: "27017"
            username: "user name here"
            password: "some password here"
            authDb: "admin"

        production:
          database:
            host: "10.197.215.216"
            port: "27017"
            username: "user name here"
            password: "some password here"
            authDb: "admin"

```

### File Based Infra 
It means that you want Hagrid to store its data that it fetches from third-party on file based database. Hagrid comes with `nitrite` db which is file based data.  Its configuration will look like below 

```yaml

---
configuration:
  connector:
    ## Path of the package where your steps, beans and assets resides. 
    step_path: "com.freshworks.hagrid.steps" 
    bean_path: "com.freshworks.hagrid.beans"
    asset_path: "com.freshworks.hagrid.assets"

  core:
    publisher:
      driver: "com.freshworks.core.publisher.MongoDb"
      thread_count: 100
      poll_count: 100

    processor:
      
      ## How many messages processor should process at once. 
      poll_count: 1000
      
      ## How many processors should run parallely. 
      number_of_parallel_processor : 20


    infra:
      
      ## Hagrid supports three kinds of infra "nitrite", "persistent" and "immemory" 
      ## nitrite means, it will use nitrite file based database. Persistent means, it will use mongodb for storing data and inmemory means, it will use RAM for the same
      infra_type: "nitrite"
      nitrite_data_path: "/Users/aaggarwal/Documents/hagrid-releases/hagrid-oss/hagrid-oss/database"
      nitrite_database_type: "file"

```

### RAM Based Infra 
It means that you want Hagrid to store its data that it fetches from third-party on RAM. Its configuration will look like below 

```yaml

---
configuration:
  connector:
    ## Path of the package where your steps, beans and assets resides. 
    step_path: "com.freshworks.hagrid.steps" 
    bean_path: "com.freshworks.hagrid.beans"
    asset_path: "com.freshworks.hagrid.assets"

  core:
    publisher:
      driver: "com.freshworks.core.publisher.MongoDb"
      thread_count: 100
      poll_count: 100

    processor:
      
      ## How many messages processor should process at once. 
      poll_count: 1000
      
      ## How many processors should run parallely. 
      number_of_parallel_processor : 20


    infra:
      
      ## Hagrid supports three kinds of infra "nitrite", "persistent" and "immemory" 
      ## nitrite means, it will use nitrite file based database. Persistent means, it will use mongodb for storing data and inmemory means, it will use RAM for the same
      infra_type: "inmemory"

```


Use `persistent` i.e. MongoDb when you are creating heavy data connectors like `discovery based connectors`
Use `im-memory` i.e. RAM when your connectors are light weight like `orchestration based connector`

<!-- HagridYAML END -->






