# Title
Refinement of Hagrid internals Services and Architecture  

## Abstract
In this DSR, I would like to detail out the refinements that we are planning to undertake as a part of release `5.0.0`

We are proposing refinements in three categories 
1. Default configuration for Hagrid   
2. Infra Layer Refinement 
3. Consumer Layer Refinements 

### Runtime Configuration 
We are planning to remove the need separate `Hagrid.yml` and allow developer to provide Hagrid configuration at run time. We are planning to undetake following items in this category
1. Runtime configuration of `step location`
2. Runtme configuration of `bean location`
   1. Scan whole project directory and look for classes which are extending `AbstractBean` or its sub-classes 
3. Runtime configuration of `asset location`
   1. Scan whole project directory and look for classes which are extending `AbstractAsset` or its sub-classes 
4. Runtime configuration of `step rate limit`
   1. Provide default rate-limit of `100 api/sec` other take it via `startSync` method parameter
5. Runtime configuration of `processor polling` 
   1. Provide default polling processor to `20` and poll item are `100` 
6. Provide unified startSync method so that we can remove multiple way to run hagrid 
7. Rename step method names for clear understanding 
8. Remove `filter` method from `steps` 
   1. We can remove filter method as filter can be done at `bean` level 

### Infra Layer Refinement 
In infra layer refinement, we are planning to undertake following items 
1. Removal of `MongoDb` and `Inmemory` - Support will be provided via `nitriteDB` file based and in memory database types
2. Allow developer to provide db type at runtime via `startSync` method 
   1. If not db is provided then `file` based in `current directory` will be considered. 
3. Improve method definitations in `InfraService` , `InfraDbQueue` , `InfraDbList` , `InfraDbKeyValue`
   1. Currently, methods like `add` are actually performing `set` . This need to be re-looked into 
4. Improve consumer and producer in infra layer 
5. Provide other methods at infra layer which can be used to filter, sort, group by any json based object stored in `InfraDbQueue`, `InfraDbList` or `InfraDbKeyValue` 
6. Infra method which set the value must take in document form like `JsonString`. This will make sure that methods like `filter` , `groupby` works on `documents` . This will make consumer service layer really flexible  

### Consumer Layer Refinement 
In consumer layer refinement, we are planning to undertake following items 
1. Remove the need for freshIndex, as it will be taken care by underline database. 
2. Provide all methods which can be used to filter, group by , sort, assets ( json strings or documents ) in document db. 
3. Improve method `streamAsset` for better usability. 
   1. Currently, AbstractAssetResponse service returns token and developer has to check if token is null or not. If it is null then developer has to break the while loop explicitly. This is not a good experience. 
4. All methods of `consumerService` should work at scale. Meaning, if customer says `getAssetByAssetType` then consumerService should not return all assets at once until asked for. 


## Table of Contents
- [Title](#title)
  - [Abstract](#abstract)
    - [Runtime Configuration](#runtime-configuration)
    - [Infra Layer Refinement](#infra-layer-refinement)
    - [Consumer Layer Refinement](#consumer-layer-refinement)
  - [Table of Contents](#table-of-contents)
  - [Introduction](#introduction)
  - [Goals and Requirements](#goals-and-requirements)
  - [Proposed Specification](#proposed-specification)
  - [Use Cases](#use-cases)
  - [Design Overview](#design-overview)
  - [Detailed Design](#detailed-design)
  - [Compatibility](#compatibility)
  - [Impact](#impact)
  - [Alternatives](#alternatives)
  - [Testing](#testing)
  - [Reference Implementation](#reference-implementation)
  - [Contributors](#contributors)
  - [Schedule](#schedule)
  - [Appendices](#appendices)

## Introduction
Background information and context for the proposal. The problem it aims to solve and why it is necessary.

## Goals and Requirements
- Objectives of the proposal.
- Specific requirements or constraints.

## Proposed Specification
Detailed technical description of the proposed solution, including APIs, interfaces, and other technical details.

## Use Cases
Examples of how the specification will be used in real-world scenarios.

Following changes need to be made 
1. From now onwards, all steps, beans and assets must be annotated with @Component and @Scope("prototype") annotation
   1. This will remove the need of step_path, bean_path and asset_path as hagrid will use spring container to fetch all beans of type List<AbstractStep> to calculate DAG and List<AbstractAsset> to calculate assetBeanDependency
2. Can dynamically pass connector Configuration in one of the startSync method
3. Removal of `filter` method from `abstractStep`
4. Unified startSync method 

## Design Overview
High-level architecture and design principles. Key components and their interactions.

## Detailed Design
In-depth technical details, including algorithms, data structures, and workflows.

## Compatibility
Discussion of backward compatibility with existing Hagrid versions or APIs. Migration strategies for existing users.

## Impact
Potential impact on the hagrid platform, developers, and users. Considerations for performance, security, etc.

## Alternatives
Other approaches considered and reasons for rejecting them.

## Testing
Plans for testing the specification. 

## Reference Implementation
Details about the reference implementation of the specification.

## Contributors
List of individuals and organizations involved in the proposals.

## Schedule
Timeline for the development and release of the specification.

## Appendices
Additional information, such as glossary, references, or related documents.
