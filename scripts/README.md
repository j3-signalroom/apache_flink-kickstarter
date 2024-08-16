# Bash scripts
The below Bash scripts are for local deployment and testing:

Name|What's it for
-|-
`force-aws-secrets-destroy.sh`|Since the Terraform configuration does not force the deletion of AWS Secrets Manager Secrets, this script will manage that task.
`run-terraform-locally.sh`|This script will log on to AWS via SSO, pass the AWS credentials to Terraform, and then execute the `Terraform`: `Init`, `Plan`, and `Apply` commands.
`run-flink-locally.sh`|This script will log on the AWS via SSO, pass the AWS credentials to the `docker-compose.yml`, and execute the `docker-compose` command.
