# Apache Flink Kickstarter

**Table of Contents**

<!-- toc -->
+ [Local MacOS Installation](#local-macos-installation)
    - [Install Apache Flink on MacOS](#install-apache-flink-on-macos)
    - [Start Cluster](#start-cluster)
    - [Lanuch the Apache Flink Dashboard](#lanuch-the-apache-flink-dashboard)
    - [Stop Cluster](#stop-cluster)
+ [Java Examples](#java-examples)
+ [Python Examples](#python-examples)
<!-- tocstop -->

## Local MacOS Installation

### Install Apache Flink on MacOS
```
$ brew install apache-flink
```

Homebrew will will typically install Apache Flink in the following folder location:
```
$ /opt/homebrew/Cellar/apache-flink/1.20.0/
```

> At the time of this writing (August 2024), version 1.20.0 was publically avaiable.

### Start Cluster
To start Apache Flink locally on your machine execute the following executable script: 
```
$ /opt/homebrew/Cellar/apache-flink/1.20.0/libexec/bin/start-cluster.sh
```

### Lanuch the Apache Flink Dashboard
To launch the Apache Flink Dashboard from your web browser, go to this URL:
```
http://localhost:8081/
```

### Stop Cluster
To stop Apache Flink locally on your machine execute the following executable script: 
```
$ /opt/homebrew/Cellar/apache-flink/1.20.0/libexec/bin/stop-cluster.sh
```

## Java Examples
[Java examples](java/README.md)

## Python Examples
[Python examples](python/README.md)
