![Hero Image](./assets/images/Hagrid%20Producer%20Consumer%20Design.png)


# Welcome

## Introduction

Hagrid is a connector development framework written in `JAVA` and is available as `SDK` which can be installed via 
`maven` or `gradle`. 

When explaining the working of Hagrid or any other concept of Hagrid through this documentation, we will take the example of `connector to fetch all users, posts, comments, community from Facebook`. 

We are taking `facebook connector` as example because almost everyone is familiar with facebook like `users`, `posts`, `comments`, `communities` etc etc.

![fb_dag.png](custom_theme/assets/images/fb_dag.png)

As per above `DAG`, we want to develop a connector which 

1. Fetches all `users` of facebook.  
2. Fetches all `posts` created by each `user` fetched above.   
3. Fetches all `comments` made on the  `post` fetched above. 
4. Fetches all `communites` created by `user` fetched above. 

**Note:** : Whenever you need to find information about `Hagrid` and not sure in which section you can find it, always use 
`search box` present in `right top corner` of this website. 

## Use cases
Hagrid is a generic connector development frameworks. It means that you can develop any kind of connector on Hagrid.

For instance, you can use `Hagrid` to develop

1. Connector to fetch `products from AWS ecommerce website`
2. Connector to fetch `Users, usages , ACL` from SSO services like `Okta`
3. Connectors to fetch `Files and ACLS` from file services like `sharepoint`
4. Connectors to fetch `schema and data` from db services like `mysql`
5. Connectors to crawl internet - yes this is also possible. 


## How does it work
Hagrid SDK provides set of `classes`, `interfaces` and `annotations` to configure the connector. Dev has to implement these 
classes, interfaces and annotations to configure the connector. 

For instance, if we take our example of `facebook connector` then Hagrid asked you to create classes for each node in the DAG. 
So you would have something like 

{%
   include-markdown "partials/steps.md"
   start="<!-- Basics START -->"
   end="<!-- Basics END -->"

%}

## Summary
With this basic idea of how Hagrid works, here I would like to summarize few things

1. `Hagrid` is generic connector development framework.
2. To develop connector on Hagrid, think in terms of `DAG` i.e. how does API calls are arranged.
3. `Each node` in `Dag` is going to be a `step` in Hagrid. 
4. `FreshHierarchy` annotation helps Hagrid to organised nodes in `DAG`. 
5. Enrich your steps by overriding method from parent class `AbstractStep` so that `Hagrid`
    1. Knows the `URL` from where to fetch the data
    2. Knows how to form next url in case of `pagination`
    3. Knows how to handle if there is `non200 http response`
    4. Filter data and attributes which is of no interest

With the basic understanding of `Dag` and `steps`, next move to [Get Started](get_start/concepts.md) to understand some more concepts of Hagrid which is useful and 
mandatory to develop a connector on Hagrid.

Please reach out to me for any suggestions and questions at [Amit Aggarwal](mailto:amit.aggarwal@freshworks.com)











