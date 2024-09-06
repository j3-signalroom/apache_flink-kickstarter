#!/bin/bash

#
# *** Script Syntax ***
# scripts/run-terraform-locally.sh <create | delete> --profile=<SSO_PROFILE_NAME> \
#                                                    --environment_name=<ENVIRONMENT_NAME> \
#                                                    --confluent_api_key=<CONFLUENT_API_KEY> \
#                                                    --confluent_api_secret=<CONFLUENT_API_SECRET> \
#                                                    --snowflake_warehouse=<SNOWFLAKE_WAREHOUSE> \
#                                                    --service_account_user=<SERVICE_ACCOUNT_USER>
#
#

# Check required command (create or delete) was supplied
case $1 in
  create)
    create_action=true;;
  delete)
    create_action=false;;
  *)
    echo
    echo "(Error Message 001)  You did not specify one of the commands: create | delete."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --environment_name=<ENVIRONMENT_NAME> --confluent_api_key=<CONFLUENT_API_KEY> --confluent_api_secret=<CONFLUENT_API_SECRET> --snowflake_warehouse=<SNOWFLAKE_WAREHOUSE> --service_account_user=<SERVICE_ACCOUNT_USER>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
    ;;
esac

# Get the arguments passed by shift to remove the first word
# then iterate over the rest of the arguments
shift
for arg in "$@" # $@ sees arguments as separate words
do
    case $arg in
        *"--profile="*)
            AWS_PROFILE=$arg;;
        *"--confluent_api_key="*)
            arg_length=20
            confluent_api_key=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--confluent_api_secret="*)
            arg_length=23
            confluent_api_secret=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--environment_name="*)
            arg_length=19
            environment_name=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--snowflake_warehouse="*)
            arg_length=22
            snowflake_warehouse=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--service_account_user="*)
            arg_length=23
            service_account_user=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
    esac
done

# Check required --profile argument was supplied
if [ -z $AWS_PROFILE ]
then
    echo
    echo "(Error Message 002)  You did not include the proper use of the --profile=<SSO_PROFILE_NAME> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --environment_name=<ENVIRONMENT_NAME> --confluent_api_key=<CONFLUENT_API_KEY> --confluent_api_secret=<CONFLUENT_API_SECRET> --snowflake_warehouse=<SNOWFLAKE_WAREHOUSE> --service_account_user=<SERVICE_ACCOUNT_USER>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --environment_name argument was supplied
if [ -z $environment_name ]
then
    echo
    echo "(Error Message 003)  You did not include the proper use of the --environment_name=<ENVIRONMENT_NAME> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --environment_name=<ENVIRONMENT_NAME> --confluent_api_key=<CONFLUENT_API_KEY> --confluent_api_secret=<CONFLUENT_API_SECRET> --snowflake_warehouse=<SNOWFLAKE_WAREHOUSE> --service_account_user=<SERVICE_ACCOUNT_USER>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --confluent_api_key argument was supplied
if [ -z $confluent_api_key ]
then
    echo
    echo "(Error Message 004)  You did not include the proper use of the --confluent_api_key=<CONFLUENT_API_KEY> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --environment_name=<ENVIRONMENT_NAME> --confluent_api_key=<CONFLUENT_API_KEY> --confluent_api_secret=<CONFLUENT_API_SECRET> --snowflake_warehouse=<SNOWFLAKE_WAREHOUSE> --service_account_user=<SERVICE_ACCOUNT_USER>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --confluent_api_secret argument was supplied
if [ -z $confluent_api_secret ]
then
    echo
    echo "(Error Message 005)  You did not include the proper use of the --confluent_api_secret=<CONFLUENT_API_SECRET> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --environment_name=<ENVIRONMENT_NAME> --confluent_api_key=<CONFLUENT_API_KEY> --confluent_api_secret=<CONFLUENT_API_SECRET> --snowflake_warehouse=<SNOWFLAKE_WAREHOUSE> --service_account_user=<SERVICE_ACCOUNT_USER>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --snowflake_warehouse argument was supplied
if [ -z $snowflake_warehouse ]
then
    echo
    echo "(Error Message 006)  You did not include the proper use of the --snowflake_user=<SNOWFLAKE_WAREHOUSE> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --environment_name=<ENVIRONMENT_NAME> --confluent_api_key=<CONFLUENT_API_KEY> --confluent_api_secret=<CONFLUENT_API_SECRET> --snowflake_warehouse=<SNOWFLAKE_WAREHOUSE> --service_account_user=<SERVICE_ACCOUNT_USER>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --service_account_user argument was supplied
if [ -z $service_account_user ]
then
    echo
    echo "(Error Message 007)  You did not include the proper use of the --service_account_user=<SERVICE_ACCOUNT_USER> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --environment_name=<ENVIRONMENT_NAME> --confluent_api_key=<CONFLUENT_API_KEY> --confluent_api_secret=<CONFLUENT_API_SECRET> --snowflake_warehouse=<SNOWFLAKE_WAREHOUSE> --service_account_user=<SERVICE_ACCOUNT_USER>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Get the SSO AWS_ACCESS_KEY_ID, AWS_ACCESS_SECRET_KEY, AWS_SESSION_TOKEN, and AWS_REGION, and
# set them as an environmental variables
aws sso login $AWS_PROFILE
eval $(aws2-wrap $AWS_PROFILE --export)
export AWS_REGION=$(aws configure get sso_region $AWS_PROFILE)
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query "Account" --output text)

# Create terraform.tfvars file
printf "aws_account_id=\"${AWS_ACCOUNT_ID}\"\
\naws_region=\"${AWS_REGION}\"\
\naws_access_key_id=\"${AWS_ACCESS_KEY_ID}\"\
\naws_secret_access_key=\"${AWS_SECRET_ACCESS_KEY}\"\
\naws_session_token=\"${AWS_SESSION_TOKEN}\"\
\nconfluent_api_key=\"${confluent_api_key}\"\
\nconfluent_api_secret=\"${confluent_api_secret}\"\
\nenvironment_name=\"${environment_name}\"\
\nsnowflake_warehouse=\"${snowflake_warehouse}\"\
\nservice_account_user=\"${service_account_user}\"" > terraform.tfvars

terraform init

if [ "$create_action" = true ]
then
    # Create/Update/Destroy the Terraform configuration
    terraform plan -var-file=terraform.tfvars
    terraform apply -var-file=terraform.tfvars
else
    # Destroy the Terraform configuration
    terraform destroy -var-file=terraform.tfvars

    # Delete the secrets created by the Terraform configurations
    aws secretsmanager delete-secret --secret-id /confluent_cloud_resource/schema_registry_cluster/java_client --force-delete-without-recovery
    aws secretsmanager delete-secret --secret-id /confluent_cloud_resource/kafka_cluster/java_client --force-delete-without-recovery
    aws secretsmanager delete-secret --secret-id '/snowflake_resource' --force-delete-without-recovery || true
    aws secretsmanager delete-secret --secret-id '/snowflake_resource/rsa_private_key_pem_1' --force-delete-without-recovery || true
    aws secretsmanager delete-secret --secret-id '/snowflake_resource/rsa_private_key_pem_2' --force-delete-without-recovery || true
fi
