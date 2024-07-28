# J3 Apache Flink API Use Cases

## Local MacOS Installation

### Install Apache Flink on MacOS
```
$ brew install apache-flink
```

### Start Cluster
```
$ /opt/homebrew/Cellar/apache-flink/1.19.1/libexec/bin/start-cluster.sh
```

### Lanuch the Apache Flink Dashboard
From your web browser, go to this URL:  `http://localhost:8081/`

### Run a Flink application
```
$ /opt/homebrew/Cellar/apache-flink/1.19.1/bin/flink
```

### Stop Cluster
```
$ /opt/homebrew/Cellar/apache-flink/1.19.1/libexec/bin/stop-cluster.sh
```


export FLINK_CONF_DIR='/opt/homebrew/Cellar/apache-flink/1.19.1/libexec/conf/'
