# Apache Flink Kickstarter

**Table of Contents**

<!-- toc -->
+ [You can call it a Flink Job or a Flink Application?](#you-can-call-it-a-flink-job-or-a-flink-application)
    - [Comprehensive Nature](#comprehensive-nature)
    - [Execution Context](#execution-context)
    - [Deployment and Operations](#deployment-and-operations)
    - [Development Perspective](#development-perspective)
    - [Ecosystem Integration](#ecosystem-integration)
    - [Terminology and Communication](#terminology-and-communication)
+ [Examples to get you kickstarted!](#examples-to-get-you-kickstarted)
    - [Java Examples](#java-examples)
    - [Python Examples](#python-examples)
+ [How to use this repo?](#how-to-use-this-repo)
    + [Start your engines!  The Apache Flink compute engine for Data Stream Processing!](#start-your-engines--the-apache-flink-compute-engine-for-data-stream-processing)
+ [Resources](#resources)
<!-- tocstop -->

## You can call it a Flink Job or a Flink Application?

> _"What's in a name? That which we call a rose by any other name would smell just as sweet." -- William Shakespeare_

Flink jobs are often called Flink applications because they encompass more than just a single task or computation. The term "application" better reflects the nature and scope of what is being developed and executed in Apache Flink. Here are several reasons why the term "Flink applications" is used:

### Comprehensive Nature
1. **Complex Workflows**: Flink jobs often represent complex workflows that include multiple stages of data processing, such as data ingestion, transformation, aggregation, and output. These workflows can be intricate and involve various interconnected operations, similar to a traditional application.

2. **Multiple Components**: A Flink job can consist of various components, such as sources, transformations, sinks, and custom functions. These components work together to perform a coherent set of tasks, much like the components of a traditional software application.

### Execution Context
3. **Runtime Environment**: When a Flink job is deployed, it runs in a distributed environment, utilizing the Flink runtime for resource management, task scheduling, and fault tolerance. This execution context is similar to how applications run on a platform or infrastructure.

4. **State Management**: Flink supports stateful stream processing, where the state is managed and maintained across different operations and executions. Managing state adds a layer of complexity and functionality typical of applications.

### Deployment and Operations
5. **Deployment**: Flink jobs are deployed to a cluster, where they run continuously (in the case of streaming jobs) or as batch jobs. This deployment and operational aspect aligns more with how applications are managed and executed rather than simple scripts or tasks.

6. **Monitoring and Maintenance**: Like applications, Flink jobs require monitoring, logging, and maintenance. They often need to handle operational concerns such as scaling, updating, and fault tolerance.

### Development Perspective
7. **Development Lifecycle**: The development of Flink jobs follows a lifecycle similar to software applications, including design, coding, testing, and deployment. Developers often use IDEs, version control systems, and CI/CD pipelines, which are standard tools for application development.

8. **Reusability and Modularity**: Flink jobs can be designed to be modular and reusable. Developers can create libraries and frameworks on top of Flink to facilitate the development of multiple jobs, much like application development.

### Ecosystem Integration
9. **Integration with Other Systems**: Flink applications often integrate with other systems and services such as databases, message queues, and cloud services. This integration is typical of applications that operate within an ecosystem of other software systems.

### Terminology and Communication
10. **Clear Communication**: Referring to Flink jobs as applications helps convey the complexity and importance of the work being done. It sets appropriate expectations for stakeholders about the scope and nature of the project.

By calling Flink jobs "Flink applications," it emphasizes the comprehensive, complex, and integrated nature of the work, aligning it more closely with how we think about and manage software applications in general.

## Examples to get you kickstarted!

### Java Examples
[Java examples](java/README.md)

### Python Examples
[Python examples](python/README.md)

## How to use this repo?
This section guides you through the local setup (on one machine but in separate containers) of the Apache Flink cluster in Session mode using Docker containers with support Apache Iceberg.  [Docker](https://www.docker.com/) is a popular container runtime that we will use to run Apache Flink on our local machine.

I created a `bash` script that executes docker-compose, a tool for defining and running multi-container applications, like our Apache Flink cluster.

### Start your engines!  The Apache Flink compute engine for Data Stream Processing!

If you are using a Mac with the M chip and it is configured with SSO to log into your AWS services, then pass these arguments using this script:
```
scripts/run-flink-locally.sh --aws-sso --chip=arm64 --profile=<PROFILE_NAME>
```

Or, for any other machine type (i.e., Linux, or PC), then pass these arguments using this script:
```
scripts/run-flink-locally.sh --aws-sso --chip=amd64 --profile=<PROFILE_NAME>
```

## Resources

[Apache Flink Glossary](https://github.com/j3-signalroom/j3-techstack-lexicon/blob/main/apache-flink-glossary.md)

[Apache Flink's Core is Dataflow Programming](https://en.wikipedia.org/wiki/Dataflow_programming)

[What is Apache Flink? — Architecture](https://flink.apache.org/what-is-flink/flink-architecture/)

[Apache Flink Use Cases](https://flink.apache.org/what-is-flink/use-cases/)

[Building Apache Flink Applications in Java](https://developer.confluent.io/courses/flink-java/overview/)
