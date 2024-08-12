# Using Apache Flink Docker
Logon to the `apache_flink-kickstarter-jobmanager-1` container's Interative Shell:
> *This allows you to interact with the container as if you were inside its terminal, enabling you to run commands, inspect the file system, or perform other tasks interactively within the container.*
```
docker exec -it -w /opt/flink/apps apache_flink-kickstarter-jobmanager-1 /bin/bash
```



```
flink run --class apache_flink.kickstarter.datastream_api.DataGeneratorApp apache_flink-kickstarter-x.xx.xx.xxx.jar
```