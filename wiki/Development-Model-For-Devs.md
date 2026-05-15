# Github Branching Model 

## Following are the branches we have 
1. `main branch` - It is the branch which will contain production code
2. `integration branch` - Branch from which all release branches will be forked. 
3. `release branch` like `3.0.0` or `3.1.0` - Branch name the version which dev are planning to release. 
4. `feature branch` like `analytics_feature` - Branches which are the feature to this release. Always checkout from `release` branch and merge into `release branch`

## Github Development Model  
1. `main` branch - It contains the squashed commits from the `release branch`. Always use `merge commit` strategy with `squash commit`. 
   1. Merge should only be done when team is sure that release branch is mature and ready for production. 
   2. If there are multiple releases going on then merge ONLY in sequence like merge release branch `3.0.0` first then `3.1.0`
2. `integration` branch - Contains the latest code.   
   1. All `release` branches always checkout from this branch **only once**. Never merge this branch back again into your release branch.  
   2. Only and Only release branches are checkout from this branch. No other branch like `feature branches` should checkout from there. 
Otherwise there are chances of code leak in higher release version into lower release version.
   3. This branch is never get merged into any branch whether `release` branch, `feature branch`
   4. `integration` branch is mainly provides a latest code for new release branches to get started. 
3. `release` branch - Contains code only for this release 
   1. Always checkout from `integration branch` only once. 
   2. Always merge this branch daily into `integration branch` so that if any one starting a new release would have your changes. 
   3. Always merge `main` branch into the release branch to make sure any latest `product release` or `hotfix` are back merged. 
   3. Never merge `integration` branch back again into this branch
4. `feature` branch - Contains code for this release branch
   1. Always checkout from `release branch` for which you are developing the code. 
   2. Always raise pull request against `release branch`


## Devs Roles & Responsibilities

1. Always make sure, to annotate your features with `@BetaRelease` or `AlphaRelease` or `ReleaseCandidates`. These annotations are available in Hagrid repo under `shared/Annotations` package. 
2. Make sure your feature is UTC written. If you are writing a new class or new method, then you have to do two things 
   1. Modify or create the `MockFacadeYourService` which enable anyone to mock your services easily. 
   2. Write unit test cases for your feature. 
3. If your feature is new and impactful, then please describe it under WIKI. 
4. You must describe your feature in ChangeLog for that release. 

