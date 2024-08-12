# Using Apache Flink Docker

> *The below Docker command starts an interactive `Bash` shell inside the `apache_flink-kickstarter-jobmanager-1` running container.  This allows you to interact with the container as if you were inside its terminal, enabling you to run commands, inspect the file system, or perform other tasks interactively within the container.*
```
docker exec -it apache_flink-kickstarter-jobmanager-1 /bin/bash
```

Change to the `apps` folder by executing the command below:'
> *This folder is mapped to the host `~/flink\apps` folder, where the built Flink apps reside.*
```
cd apps
```


```
flink run --class apache_flink.kickstarter.datastream_api.DataGeneratorApp apache_flink-kickstarter-x.xx.xx.xxx.jar
```