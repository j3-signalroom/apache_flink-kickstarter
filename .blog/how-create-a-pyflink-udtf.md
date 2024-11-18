# How to create a User-Defined Table Function (UDTF) in PyFlink to fetch data from an external source for your Flink App?

Here is my scenario: I needed to convert one of my Flink apps from Java to Python. However, I encountered a challenge: I used a custom DataStream source in my [Java code](../java/app/src/main/java/kickstarter/ConfluentClientConfigurationLookup.java). The custom DataStream source aims to communicate with the AWS Secrets Manager and AWS Systems Manager Parameter Store to securely retrieve Kafka configuration details for the KafkaSource and KafkaSink operators in the Flink apps.

After some struggles, I found that it was not a one-to-one conversion into Python, and indeed, I needed to use a different architectural approach to achieve the same functionality.

![frustrated-pulling-hair](images/frustrated-pulling-hair.gif)


But then, Martijn Visser—shoutout to the Group Product Manager at Confluent—came to the rescue and suggested a different approach: a Python Table API User-Defined Function (UDF). I thought, "Cool, but... how?!"

![puzzle-jigsaw](images/puzzle-jigsaw.gif)

What followed was a classic journey of web searches, chatting with LLMs, scouring Apache Flink documentation, and assembling a puzzle with a million nearly identical pieces. 🧭

And guess what? After much trial and error, I cracked the code! You can check it out in my GitHub repo [here]( https://github.com/j3-signalroom/apache_flink-kickstarter/blob/main/python/kickstarter/helper/kafka_properties_udtf.py)!

![matrix-brilliant](images/matrix-brilliant.gif)

This article is here to share the solution, step-by-step, with you!

The magic happens in the `kafka_properties_udtf.py module`, which contains the User-Defined Table Function (UDTF) `KafkaProperties` class, along with the `execute_kafka_properties_udtf()` method that kicks it off. Let's dive in and unravel it all together!

## The Class (`KafkaProperties`)
The class is a User-Defined Table Function (UDTF), used to retrieve the Kafka Cluster properties from the AWS Secrets Manager and Parameter Store.  Below is a step-by-step breakdown of the class:

### Step 1 of 5 --- Class declaration
```python
class KafkaProperties(TableFunction):
    """This User-Defined Table Function (UDTF) is used to retrieve the Kafka Cluster properties
    from the AWS Secrets Manager and Parameter Store.
    """
    ...
```
The `KafkaProperties` class, which is the User-Defined Table Function (UDTF), retrieves the Kafka Cluster properties from the AWS Secrets Manager and Parameter Store, which inherits from the PyFlink [TableFunction](https://nightlies.apache.org/flink/flink-docs-release-1.20/api/python/reference/pyflink.table/api/pyflink.table.udf.TableFunction.html?highlight=tablefunction), a base interface for a user-defined table function that creates zero or more row values.

### Step 2 of 5 --- Constructor(`__init__()`)
```python
def __init__(self, for_consumer: bool, service_account_user: str):
    """Initializes the UDTF with the necessary parameters.

    Args:
        for_consumer (bool): determines if the Kafka Client is a consumer or producer.
        service_account_user (str): is the name of the service account user.  It is used in
        the prefix to the path of the Kafka Cluster secrets in the AWS Secrets Manager and
        the Kafka Client parameters in the AWS Systems Manager Parameter Store.
    """
    self._for_consumer = for_consumer
    self._service_account_user = service_account_user
    self._aws_region_name = os.environ['AWS_REGION']
```

The Constructor initializes the class with two parameters, which then set the:
- `for_consumer` (`bool`):  Indicates whether the Kafka client is a consumer or a producer.
- `service_account_user` (`str`):  Used in the prefix for constructing paths to secrets and configurations.
- `aws_region_name` (`str`):  Is obtained from the environment variable, `AWS_REGION`, which is needed for interacting with AWS Services.

### Step 3 of 5 --- UDTF Evaluation Method (`eval()`)
```python
def eval(self, kakfa_properties: Row) -> Iterator[Row]:
    """This method retrieves the Kafka Cluster properties from the AWS Secrets Manager 
    and AWS Systems Manager.

    Args:
        kakfa_properties (Row): is a Row object that contains the Kafka Cluster properties.

    Raises:
        RuntimeError: is a generic error that is raised when an error occurs.

    Yields:
        Iterator[Row]: combination of Kafka Cluster properties and Kafka Client parameters.
    """
    # Get the Kafka Client properties from AWS Secrets Manager and AWS Systems Manager Parameter Store.
    secret_path_prefix = f"/confluent_cloud_resource/{self._service_account_user}"

    properties = self.get_kafka_properties(
        f"{secret_path_prefix}/kafka_cluster/java_client",
        f"{secret_path_prefix}/consumer_kafka_client" if self._for_consumer else f"{secret_path_prefix}/producer_kafka_client"
    )
    if properties is None:
        raise RuntimeError(f"Failed to retrieve the Kafka Client properties from '{secret_path_prefix}' secrets because {properties.get_error_message_code()}:{properties.get_error_message()}")
    else:
        for property_key, property_value in properties.items():
            yield Row(str(property_key), str(property_value))
```

The base class provides a set of methods that can be overriden such as `open()`, `close()`, and `is_deterministic()`.  But, the `eval()` is the _**heart**_ of the UDFT and is where I place the calls to the AWS Services.  The method constructs paths for cluster secrets and client parameters based on the service account name and type of client (consumer or producer).  Then, it calls `get_kafka_properties()` to retrieve all the properties, which are then yielded as key-value pairs in Row format.

### Step 4 of 5 --- `get_kafka_properties()` method
```python
def get_kafka_properties(self, cluster_secrets_path: str, client_parameters_path: str) -> tuple[str, str]:
    """This method returns the Kafka Cluster properties from the AWS Secrets Manager and Parameter Store.

    Args:
        cluster_secrets_path (str): the path to the Kafka Cluster secrets in the AWS Secrets Manager.
        client_parameters_path (str): the path to the Kafka Client parameters in the AWS Systems Manager
        Parameter Store.

    Returns:
        properties (tuple[str, str]): the Kafka Cluster properties collection if successful, otherwise None.
    """
    properties = {}

    # Retrieve the SECRET properties from the AWS Secrets Manager
    secret = self.get_secrets(cluster_secrets_path)
    if secret is not None:
        try:
            # Convert the JSON object to a dictionary
            secret_data = secret
            for key in secret_data:
                properties[key] = secret_data[key]

        except json.JSONDecodeError as e:
            return None

        # Retrieve the parameters from the AWS Systems Manager Parameter Store
        parameters = self.get_parameters(client_parameters_path)
        if parameters is not None:
            for key in parameters:
                properties[key] = parameters[key]
            return properties
        else:
            return None
    else:
        return None
```

Method arguments:
+ `cluster_secrets_path` (`str`): The path to Kafka cluster secrets in AWS Secrets Manager.
+ `client_parameters_path` (`str`): The path to client parameters in AWS Systems Manager.

The method calls two helper methods (`get_secrets()` and `get_parameters()`) to fetch the respective information and combine them into a dictionary, which it returns.

#### Step 5a of 5b --- `get_secrets()` helper method
```python
def get_secrets(self, secrets_name: str) -> (dict):
    """This method retrieve secrets from the AWS Secrets Manager.
    
    Arg(s):
        secrets_name (str): Pass the name of the secrets you want the secrets for.
        
    Return(s):
        If successful, returns a JSON object of the secrets' value(s) stored.  Otherwise,
        the method has failed and 'None' is returned.
        
    Raise(s):
        DecryptionFailureException: Secrets Manager can't decrypt the protected secret text
        using the provided KMS key.
        InternalServiceErrorException: An internal server error exception object.
        InvalidParameterException: An input parameter violated a constraint.
        InvalidRequestException: Indicates that something is wrong with the input to the request.
        ResourceNotFoundExceptionAttributeError: The operation tried to access a keyspace or table
        that doesn't exist. The resource might not be specified correctly, or its status might not
        be ACTIVE.
    """
    # Create a Secrets Manager client
    session = boto3.session.Session()
    client = session.client(service_name='secretsmanager', region_name=self._aws_region_name)
    
    logging.info("AWS_ACCESS_KEY_ID: %s", os.environ['AWS_ACCESS_KEY_ID'])
    
    try:
        get_secret_value_response = client.get_secret_value(SecretId=secrets_name)
        
        # Decrypts secret using the associated KMS (Key Management System) CMK (Customer Master Key).
        return json.loads(get_secret_value_response['SecretString'])
    except ClientError as e:
        logging.error("Failed to get secrets (%s) from the AWS Secrets Manager because of %s.", secrets_name, e)
        if e.response['Error']['Code'] == 'DecryptionFailureException' or \
            e.response['Error']['Code'] == 'InternalServiceErrorException' or \
            e.response['Error']['Code'] == 'InvalidParameterException' or \
            e.response['Error']['Code'] == 'InvalidRequestException' or \
            e.response['Error']['Code'] == 'ResourceNotFoundException':
                raise ValueError(e.response['Error']['Code'])
        return None
```

Method argument:
+ `secrets_name` (`str`):  The name of the secrets you want the secret values for.

This helper method retrieves secrets from the AWS Secrets Manager and, if successful, retrieves the secret, parses it from JSON, and returns it.

#### Step 5b of 5b --- `get_parameters()` helper method
```python
def get_parameters(self, parameter_path: str) -> (dict):
    """This method retrieves the parameteres from the System Manager Parameter Store.
    Moreover, it converts the values to the appropriate data type.
    
    Arg(s):
        parameter_path (str): The hierarchy for the parameter.  Hierarchies start
        with a forward slash (/). The hierarchy is the parameter name except the last
        part of the parameter.  For the API call to succeed, the last part of the
        parameter name can't be in the path. A parameter name hierarchy can have a
        maximum of 15 levels.
        
    Return(s):
        parameters (dict): Goes throught recursively and returns all the parameters
        within a hierarchy.
    """        
    session = boto3.session.Session()
    client = session.client(service_name='ssm', region_name=self._aws_region_name)
    
    try:
        response = client.get_parameters_by_path(Path=parameter_path, Recursive=False, WithDecryption=True)
    except ClientError as e:
        logging.error("Failed to get parameters from the AWS Systems Manager Parameter Store because of %s.", e)
        raise ValueError(e.response['Error']['Code'])
    else:
        parameters = {}
        for parameter in response['Parameters']:
            # Get the value of the parameter that will constitutes the key for the dictionary
            key = parameter['Name'][parameter['Name'].rfind('/') + 1:]
            
            # By default assume the parameter value is a string data type
            value = "" + parameter['Value'] + ""
            
            # Check if the value has zero decimal points, if so, maybe it's an integer
            # if not, go with the default string value
            if parameter['Value'].count('.') == 0:
                try:
                    value = int(parameter['Value'].replace(',',''))
                except Exception:
                    pass
            # Check if the value has only one decimal point, if so, maybe it's a float
            # if not, go with the default string value
            elif parameter['Value'].count('.') == 1:
                try:
                    value = float(sub(r'[^\d.]', '', parameter['Value']))
                except Exception:
                    pass
                
            parameters[key] = value
            
        return parameters
```

Method argument:
+ `parameter_path` (`str`):  The path name to client parameters in AWS Systems Manager.

This helper method retrieves the parameters from the AWS System Manager Parameter Store. Converts the parameter values to their appropriate data types (e.g., `int`, `float`, or `str`). (It does this because AWS only stores parameter values as a `string` data type.) If successful, the parameters are returned as a dictionary.


## The method (`execute_kafka_properties_udtf()`)
The function `execute_kafka_properties_udtf()` is designed to retrieve Kafka cluster properties by triggering the `KafkaProperties` User-Defined Table Function (UDTF) in a PyFlink environment. 

```python
def execute_kafka_properties_udtf(tbl_env: StreamTableEnvironment, for_consumer: bool, service_account_user: str) -> dict:
    """This method retrieves the Kafka Cluster properties from the AWS Secrets Manager 
    and AWS Systems Manager.

    Args:
        tbl_env (TableEnvironment): is the instantiated Flink Table Environment.  A Table
        Environment is a unified entry point for creating Table and SQL API programs.  It
        is used to convert a DataStream into a Table or vice versa.  It is also used to
        register a Table in a catalog.
        for_consumer (bool): determines if the Kafka Client is a consumer or producer.
        service_account_user (str): is the name of the service account user.  It is used in
        the prefix to the path of the Kafka Cluster secrets in the AWS Secrets Manager and
        the Kafka Client parameters in the AWS Systems Manager Parameter Store.

    Returns:
        dict: combination of Kafka Cluster properties and Kafka Client parameters.
    """
    # Define the schema for the table and the return result of the UDTF
    schema = DataTypes.ROW([
        DataTypes.FIELD('property_key', DataTypes.STRING()),
        DataTypes.FIELD('property_value', DataTypes.STRING())
    ])

    # Create an empty table
    kafka_property_table = tbl_env.from_elements([('','')], schema)

    # Define the table name based on the type of Kafka client
    table_name = "kafka_property_table_" + ("consumer" if for_consumer else "producer")

    # Register the table as a temporary view
    tbl_env.create_temporary_view(table_name, kafka_property_table)

    # Get the table from the temporary view
    kafka_property_table = tbl_env.from_path(table_name)

    # Register the Python function as a PyFlink UDTF (User-Defined Table Function)
    kafka_properties_udtf = udtf(f=KafkaProperties(for_consumer, service_account_user), 
                                 result_types=schema)

    # Join the Kafka Property Table with the UDTF
    func_results = kafka_property_table.join_lateral(kafka_properties_udtf.alias("key", "value")).select(col("key"), col("value"))

    # Convert the result into a Python dictionary
    result = func_results.execute().collect()
    result_dict = {}
    for row in result:
        result_dict[row[0]] = row[1]
    result.close()

    # Return the table results into a dictionary
    return result_dict

```

Method argument(s):
- `tbl_env` (`StreamTableEnvironment`):  Represents the Flink Table Environment, which serves as an entry point for working with tables and SQL queries in PyFlink, which is specifically used to create and register tables and to execute SQL-like operations.
- `for_consumer` (`bool`):  Indicates whether the Kafka client is a consumer or producer. This flag determines which properties to retrieve (consumer-specific or producer-specific).
- `service_account_user` (`str`):  The name of the service account user used in the prefix to determine paths to secrets and parameters.

Below is a step-by-step breakdown of the method:

### Step 1 of 8 --- Define the Schema for the UDTF Table result
```python
schema = DataTypes.ROW([
    DataTypes.FIELD('property_key', DataTypes.STRING()),
    DataTypes.FIELD('property_value', DataTypes.STRING())
])
```

The schema defines the structure of the UDTF Table rows.  The schema dictates that the row will have two fields:  `property_key` and `property_value`, both of data type `STRING`.

### Step 2 of 8 --- Create an Empty Table
```python
kafka_property_table = tbl_env.from_elements([('','')], schema)
```

This creates an empty table with the defined schema in the Flink environment. To provide structure, the table must be initialized with a placeholder row.

### Step 3 of 8 --- Define the Table Name and Register it as a Temporary View
```python
table_name = "kafka_property_table_" + ("consumer" if for_consumer else "producer")
tbl_env.create_temporary_view(table_name, kafka_property_table)
```

A unique name is generated for the table based on the type of Kafka client (`consumer` or `producer`).  The table is then registered as a temporary view that can be queried later.

### Step 4 of 8 --- Retrieve the Table from the Temporary View
```python
kafka_property_table = tbl_env.from_path(table_name)
```

This line retrieves the table registered earlier as a temporary view, allowing further operations to be performed.

### Step 5 of 8 --- Register and Apply the UDTF (`KafkaProperties`)
```python
kafka_properties_udtf = udtf(f=KafkaProperties(for_consumer, service_account_user), result_types=schema)
```

The `KafkaProperties` UDTF is instantiated with the appropriate parameters (`for_consumer` and `service_account_user`).  The udtf() function registers the UDTF with the output schema defined earlier.

### Step 6 of 8 --- Join the Table with the UDTF
```python
func_results = kafka_property_table.join_lateral(kafka_properties_udtf.alias("key", "value")).select(col("key"), col("value"))
```

The `join_lateral()` function applies the UDTF to each row of the `kafka_property_table`.  The result (`func_results`) contains the output rows with properties retrieved by the UDTF.  `alias("key", "value")` is used to assign column names to the results produced by the UDTF.

### Step 7 of 8 --- Convert the Result into a Python Dictionary
```python
result = func_results.execute().collect()
result_dict = {}
for row in result:
    result_dict[row[0]] = row[1]
result.close()
```

The `func_results.execute().collect()` statement executes the UDTF in the Flink environment and collects the result into a list of rows.  A dictionary (`result_dict`) is created by iterating over the rows and adding each property key and value as key-value pairs in the dictionary.  Finally, the result is closed to release any resources.

### Step 8 of 8 --- Return the Result as a Python Dictionary
```python
return result_dict
```

The result dictionary (`result_dict`) containing Kafka properties is returned by the function.

## Watch the UDTF in Action: Streamlining Kafka Integration!
```python
def main(args):
    """The entry point to the Flight Importer Flink App (a.k.a., Flink job graph --- DAG).
        
    Args:
        args (str): is the arguments passed to the script.
    """
    # Create a blank Flink execution environment
    env = StreamExecutionEnvironment.get_execution_environment()

    # Create a Table Environment
    tbl_env = StreamTableEnvironment.create(stream_execution_environment=env)

    # Get the Kafka Cluster properties for the Kafka consumer client
    consumer_properties = execute_kafka_properties_udtf(tbl_env, True, args.s3_bucket_name)

    # Sets up a Flink Kafka source to consume data from the Kafka topic `airline.skyone`
    skyone_source = (KafkaSource.builder()
                                .set_properties(consumer_properties)
                                .set_topics("airline.skyone")
                                .set_group_id("skyone_group")
                                .set_starting_offsets(KafkaOffsetsInitializer.earliest())
                                .set_value_only_deserializer(JsonRowDeserializationSchema
                                                                .builder()
                                                                .type_info(AirlineFlightData.get_value_type_info())
                                                                .build())
                                .build())
    
    ...

```
In the snippet above, the `execute_kafka_properties_udtf()` method plays a pivotal role in your Flink application.  It seamlessly retrieves Kafka Consumer Client properties and channels them into the `KafkaSource` operator.  This approach ensures that properties are handled securely, paving the way for effortless scalability while maintaining data integrity.  By streamlining the integration of Kafka client configuration with Flink, you're building a robust, scalable, and future-proof data pipeline that empowers real-time processing.

## Summary
The `KafkaProperties` class and `execute_kafka_properties_udtf()` method forms a practical solution for dynamically retrieving Kafka configuration properties in a PyFlink streaming environment.  By integrating AWS Secrets Manager and AWS Systems Manager Parameter Store, this architecture ensures that Kafka client configurations are managed with both scalability and security in mind—enabling seamless, real-time adjustments while safeguarding sensitive information.  This approach exemplifies a modern, cloud-native way of handling configuration management, perfectly tailored for robust and adaptive data streaming workflows.
