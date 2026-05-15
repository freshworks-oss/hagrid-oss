# Design & Inspirations

While designing Hagrid, we include lots of proven designs and create custom data structures like below

#### Designs Inspiration

1. Kafka Streams as `Processor & Publisher Queue` in Hagrid
2. Kafka Topology as `DagService` in Hagrid
3. Kafka Joins as `@FreshJoin` in Hagrid
4. Kafka Topics as `ProcessorQueue, PublisherQueue, TraverserList` in Hagrid
5. Redis Table as `Key_value` in Hagrid
6. Informatica - as `Traverser & Processor Service` in Hagrid
7. Spring Boot - as `IOC Container` in Hagrid

#### Data structure Used
1. Tree Traversal for Dag Traversal
2. Bloom Filters for efficient lookups
3. Semaphores for managing concurrency
4. Queue, List & Key Value implementation on the top of MongoDB
5. Lock free thread Safe classes using `CAS` instructions