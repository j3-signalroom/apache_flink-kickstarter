# Apache Flink Glossary

## DAG (Directed Acyclic Graph)
A DAG in Apache Flink stands for Directed Acyclic Graph.  It is a fundamental concept used to represent the series of transformations applied to the data in a Flink program.  Each node in the DAG represents a computational operation or transformation, while the directed edges indicate the data flow between these operations.  The graph is acyclic, meaning it does not contain any cycles, ensuring that there is a clear, non-repetitive flow of data from the source to the sink.

### Components of a DAG
1. **Vertices (Nodes)**:  Each vertex represents a data transformation or a computational operation.  This could be a map, filter, join, window operation, etc.

2. **Edges**:  The directed edges indicate the data flow dependencies between the vertices.  An edge from vertex A to vertex B means that the output of operation A is used as input for operation B.

3. **Sources**:  Nodes with no incoming edges.  These represent the starting points of the data flow, usually corresponding to data sources like Kafka topics, files, or databases.

4. **Sinks**:  Nodes with no outgoing edges. These represent the end points of the data flow, where the final results are written to external systems like databases, files, or message queues.

## JobGraph
The JobGraph represents a Flink dataflow program, at the low level that the [JobManager](#jobmanager) accepts.  All programs from higher level APIs are transformed into JobGraphs.  The JobGraph is a graph of vertices and intermediate results that are connected together to form a [DAG](#dag-directed-acyclic-graph).

## JobManager
The JobManager is an orchestrator. It is responsible for coordinating and managing different activities within a Flink application.  This includes scheduling tasks, coordinating checkpoints, and handling the execution of code (directed graphs called [JobGraphs](#jobgraph)).

## TaskManager
The TaskManager is responsible for executing the tasks assigned by the [JobManager](#jobmanager).  It has a set of slots (the smallest unit of resource scheduling) that allows it to execute multiple tasks in separate threads.