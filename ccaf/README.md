# Flink Applications Powered by Python on Confluent Cloud for Apache Flink (CCAF)
[Confluent Cloud for Apache Flink (CCAF)](https://docs.confluent.io/cloud/current/flink/overview.html) integrates Confluent Cloud, a fully managed Apache Kafka service, with Apache Flink, a powerful stream processing framework. This integration enables real-time data processing, analytics, and complex event processing on data streams managed by Confluent Cloud.  Your Kafka topics appear automatically as queryable Flink tables, with schemas and metadata attached by Confluent Cloud.

![flink-kafka-ecosystem](../.blog/images/flink-kafka-ecosystem.png)

Confluent Cloud for Apache Flink supports creating stream-processing applications by using Flink SQL, the [Flink Table API](https://docs.confluent.io/cloud/current/flink/reference/table-api.html#flink-table-api) (Java and Python), and custom [user-defined functions](https://docs.confluent.io/cloud/current/flink/concepts/user-defined-functions.html#flink-sql-udfs).

**Table of Contents**

<!-- toc -->
+ [1.0 Deploying Apache Flink Applications on Confluent Cloud’s Fully Managed Platform](#10-deploying-apache-flink-applications-on-confluent-clouds-fully-managed-platform)
  * [1.1 Running the app locally using a script](#11-running-the-app-locally-using-a-script)
  * [1.2 Running the app locally in a Docker container](#12-running-the-app-locally-in-a-docker-container)
  * [1.3 Did you peek into any of the scripts, and noticed I prepended `uv run` to `flight_consolidator`?](#13-did-you-peek-into-any-of-the-scripts-and-noticed-i-prepended-uv-run-to-flight_consolidator)
+ [2.0 Resources](#20-resources)
<!-- tocstop -->


## 1.0 Deploying Apache Flink Applications on Confluent Cloud’s Fully Managed Platform

> _Because Flink applications require AWS SSO credentials to run, I created Bash scripts that retrieve and inject these credentials. This approach ensures the necessary authentication is in place, allowing the Flink applications to execute successfully._

### 1.1 Running the app locally using a script
Flink App|Run Script
-|-
**`avro_flight_consolidator_app`**|`scripts/run-flight-consolidator-ccaf-app-locally.sh --profile=<AWS_SSO_PROFILE_NAME> --catalog-name=<CATALOG_NAME> --database-name=<DATABASE_NAME>`

> Argument placeholder|Replace with
> -|-
> `<AWS_SSO_PROFILE_NAME>`|your AWS SSO profile name for your AWS infrastructue that host your AWS Secrets Manager.
> `<CATALOG_NAME>`|the Environment name of the Kafka Cluster.
> `<DATABASE_NAME>`|the Database name of the Kafka Cluster.

### 1.2 Running the app locally in a Docker container
Flink App|Run Script
-|-
**`avro_flight_consolidator_app`**|`scripts/run-flight-consolidator-ccaf-docker-locally.sh --profile=<AWS_SSO_PROFILE_NAME> --catalog-name=<CATALOG_NAME> --database-name=<DATABASE_NAME>`

> Argument placeholder|Replace with
> -|-
> `<AWS_SSO_PROFILE_NAME>`|your AWS SSO profile name for your AWS infrastructue that host your AWS Secrets Manager.
> `<CATALOG_NAME>`|the Environment name of the Kafka Cluster.
> `<DATABASE_NAME>`|the Database name of the Kafka Cluster.

### 1.3 Did you peek into any of the scripts, and noticed I prepended `uv run` to `flight_consolidator`?
You maybe asking yourself why.  Well, `uv` is an incredibly fast Python package installer and dependency resolver, written in [**Rust**](https://github.blog/developer-skills/programming-languages-and-frameworks/why-rust-is-the-most-admired-language-among-developers/), and designed to seamlessly replace `pip`, `pipx`, `poetry`, `pyenv`, `twine`, `virtualenv`, and more in your workflows. By prefixing `uv run` to a command, you're ensuring that the command runs in an optimal Python environment.

Now, let's go a little deeper into the magic behind `uv run`:
- When you use it with a file ending in `.py` or an HTTP(S) URL, `uv` treats it as a script and runs it with a Python interpreter. In other words, `uv run file.py` is equivalent to `uv run python file.py`. If you're working with a URL, `uv` even downloads it temporarily to execute it. Any inline dependency metadata is installed into an isolated, temporary environment—meaning zero leftover mess! When used with `-`, the input will be read from `stdin`, and treated as a Python script.
- If used in a project directory, `uv` will automatically create or update the project environment before running the command.
- Outside of a project, if there's a virtual environment present in your current directory (or any parent directory), `uv` runs the command in that environment. If no environment is found, it uses the interpreter's environment.

So what does this mean when we put `uv run` before `flight_consolidator`? It means `uv` takes care of all the setup—fast and seamless—right in your local Docker container. If you think AI/ML is magic, the work the folks at [Astral](https://astral.sh/) have done with `uv` is pure wizardry!

Curious to learn more about [Astral](https://astral.sh/)'s `uv`? Check these out:
- Documentation: Learn about [`uv`](https://docs.astral.sh/uv/).
- Video: [`uv` IS THE FUTURE OF PYTHON PACKING!](https://www.youtube.com/watch?v=8UuW8o4bHbw)

## 2.0 Resources
[Table API on Confluent Cloud for Apache Flink](https://docs.confluent.io/cloud/current/flink/reference/table-api.html#table-api-on-af-long)

[Table API in Confluent Cloud for Apache Flink API Function](https://docs.confluent.io/cloud/current/flink/reference/functions/table-api-functions.html#flink-table-api-functions)

[Information Schema in Confluent Cloud for Apache Flink](https://docs.confluent.io/cloud/current/flink/reference/flink-sql-information-schema.html)
