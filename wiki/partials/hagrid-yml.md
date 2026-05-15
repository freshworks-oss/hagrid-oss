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

    traverser:
      thread_count: 1
      type: "http"

    join:
      inner_join:
        driver: "com.freshworks.core.processor.joins.InnerJoinService"

      left_join:
        driver: "com.freshworks.core.processor.joins.LeftJoinService"

      noop_join:
        driver: "com.freshworks.core.processor.joins.NoopJoinService"


    infra:
      
      ## Hagrid supports three kinds of infra "h2", "persistent" and "immemory" 
      ## H2 means, it will use h2 file based database. Persistent means, it will use mongodb for storing data and inmemory means, it will use RAM for the same
      infra_type: "h2"
      h2_data_path: "/Users/aaggarwal/Documents/hagrid-releases/data/hagrid-3.7.0/hagrid-db-file"
      h2_database_type: "file"
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

      redis:
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

      infra: "com.freshworks.core.shared.MongoDb.infra.MongoDbInfraService"
      queue_driver: "com.freshworks.core.infra.MongoDb.MongodbQueue"
      list_driver: "com.freshworks.core.infra.MongoDb.MongodbList"
      key_value_driver: "com.freshworks.core.infra.MongoDb.MongodbKeyValue"

    generation:
      strategy: "asap" # As of now, we support just asap strategy. There is no what-complete
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
Out of the box, `hagrid` supports two kinds of `Infra`. 

Configuration `infra.infra_type` could be 

1. persistent - Here `Hagrid` expects `Mongodb` configuration to be provided so that it can save and retrieve data during sync.   
2. inmemory -  Here no extra configuration is needed.

Use `persistent` i.e. MongoDb when you are creating heavy data connectors like `discovery based connectors`
Use `im-memory` i.e. RAM when your connectors are light weight like `orchestration based connector`

<!-- HagridYAML END -->






