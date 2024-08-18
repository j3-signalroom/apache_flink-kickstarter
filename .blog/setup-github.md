# Set up GitHub Secrets

**Table of Contents**

<!-- toc -->
+ [Terraform Cloud API token](#terraform-cloud-api-token)
+ [Confluent Cloud API](#confluent-cloud-api)
<!-- tocstop -->

## Terraform Cloud API token
From the [Tokens page](https://app.terraform.io/app/settings/tokens), create/update the API token and store it in the [AWS Secrets Manager](https://us-east-1.console.aws.amazon.com/secretsmanager/secret?name=%2Fsi-iac-confluent_cloud_kafka_api_key_rotation-tf%2Fconfluent&region=us-east-1).  Then add/update the `TF_API_TOKEN` secret on the [GitHub Action secrets and variables, secret tab](https://github.com/j3-signalroom/apache_flink-kickstarter/settings/secrets/actions).

## Confluent Cloud API
Confluent Cloud requires API keys to manage access and authentication to different parts of the service.  An API key consists of a key and a secret.  You can create and manage API keys by using the [Confluent Cloud CLI](https://docs.confluent.io/confluent-cli/current/overview.html).  Learn more about Confluent Cloud API Key access [here](https://docs.confluent.io/cloud/current/access-management/authenticate/api-keys/api-keys.html#ccloud-api-keys).

Using the Confluent CLI, execute the follow command to generate the Cloud API Key:
```
confluent api-key create --resource "cloud" 
```
Then, for instance, copy-and-paste the API Key and API Secret values to the respective, `CONFLUENT_CLOUD_API_KEY` and `CONFLUENT_CLOUD_API_SECRET` secrets, that need to be created/updated on the [GitHub Action secrets and variables, secret tab](https://github.com/j3-signalroom/apache_flink-kickstarter/settings/secrets/actions).
