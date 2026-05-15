# Advance Concepts


## In Detail

### DAG ( Directed Acyclic Graph)
DAG represents the way in which information can be fetched from `third-party` system.

**Facebook Connector**

DAG to fetch the data from facebook may look like this based on your use case.

![fb_dag.png](../custom_theme/assets/images/fb_dag.png)


**Mysql Connector**

Dag to fetch the `tables and Records` from `mysql` may look like this

![mysql_dag.png](../custom_theme/assets/images/mysql_dag.png)



### Steps & Methods
A `step` in a `Hagrid` is a class which extends either `HttpAbstractStep` or `NonHttpAbstractStep` and it is a representation of a `node` in `DAG`.
So if we take an example of `Facebook connector` DAG then you would have 4  http based steps `steps` like described

{%
include-markdown "partials/steps.md"
start="<!-- Basics START -->"
end="<!-- Basics END -->"

%}

Below is the list of the methods that should be overridden in each step

{%
include-markdown "partials/steps.md"
start="<!-- List of Methods START -->"
end="<!-- List of Methods END -->"
heading-offset=3

%}


### Beans

{%
include-markdown "partials/beans.md"
start="<!-- Beans START -->"
end="<!-- Beans END -->"

%}


### Assets

{%
include-markdown "partials/assets.md"
start="<!-- Assets BASIC START -->"
end="<!-- Assets BASIC END -->"
%}