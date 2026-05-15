# Error handling Use cases
In this section, I list down various use cases where program control flow does not follow the happy execution path. It is usually
in the case when certain condition is not met for the control flow to execute on happy path.


## Shutdown Hagrid 
We have encounter the use cases where developer want to shutdown whole Hagrid when a particular condition is not met. 
For example - third-party API is not responding. In such cases, developers can do the following 

1. Fetch `SyncService` from `SyncServiceContainer`
2. Call `syncService.interruptSync` to interrupt the Hagrid ( both processor and traverser module). 


## Shutdown Traverser but not Processor
There are use cases where developer do not want to terminate the whole Hagrid. Developer want to terminate just `Traverser` 
module so that it does not fetch any further data, however processor module should process ( convert beans into assets) of 
whatever has already been received in the system.
In such cases, developers can do the following 

1. Fetch `DagTraverserService` from `SyncServiceContainer`
2. Call `dagTraverserService.interruptSync()` to stop just Traverser. 




