#!/bin/bash

#
# *** Script Syntax ***
# scripts/run-terraform-locally.sh <create | delete> --profile=<SSO_PROFILE_NAME> \
#                                                    --confluent-api-key=<CONFLUENT_API_KEY> \
#                                                    --confluent-api-secret=<CONFLUENT_API_SECRET> \
#                                                    --snowflake-warehouse=<SNOWFLAKE_WAREHOUSE> \
#                                                    --service-account-user=<SERVICE_ACCOUNT_USER> \
#                                                    --day-count=<DAY_COUNT> \
#                                                    --auto-offset-reset=<earliest | latest> \
#                                                    --number-of-api-keys-to-retain=<NUMBER_OF_API_KEYS_TO_RETAIN>
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
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --confluent-api-key=<CONFLUENT_API_KEY> --confluent-api-secret=<CONFLUENT_API_SECRET> --snowflake-warehouse=<SNOWFLAKE_WAREHOUSE> --service-account-user=<SERVICE_ACCOUNT_USER> --day-count=<DAY_COUNT> --auto-offset-reset=<earliest | latest> --number-of-api-keys-to-retain=<NUMBER_OF_API_KEYS_TO_RETAIN>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
    ;;
esac

# Get the arguments passed by shift to remove the first word
# then iterate over the rest of the arguments
auto_offset_reset_set=false
shift
for arg in "$@" # $@ sees arguments as separate words
do
    case $arg in
        *"--profile="*)
            AWS_PROFILE=$arg;;
        *"--confluent-api-key="*)
            arg_length=20
            confluent_api_key=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--confluent-api-secret="*)
            arg_length=23
            confluent_api_secret=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--snowflake-warehouse="*)
            arg_length=22
            snowflake_warehouse=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--service-account-user="*)
            arg_length=23
            service_account_user=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--day-count="*)
            arg_length=12
            day_count=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        --auto-offset-reset=earliest)
            auto_offset_reset_set=true
            auto_offset_reset="earliest";;
        --auto-offset-reset=latest)
            auto_offset_reset_set=true
            auto_offset_reset="latest";;
        *"--number-of-api-keys-to-retain="*)
            arg_length=31
            number_of_api_keys_to_retain=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
    esac
done

# Check required --profile argument was supplied
if [ -z $AWS_PROFILE ]
then
    echo
    echo "(Error Message 002)  You did not include the proper use of the --profile=<SSO_PROFILE_NAME> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --confluent-api-key=<CONFLUENT_API_KEY> --confluent-api-secret=<CONFLUENT_API_SECRET> --snowflake-warehouse=<SNOWFLAKE_WAREHOUSE> --service-account-user=<SERVICE_ACCOUNT_USER> --day-count=<DAY_COUNT> --auto-offset-reset=<earliest | latest> --number-of-api-keys-to-retain=<NUMBER_OF_API_KEYS_TO_RETAIN>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --confluent-api-key argument was supplied
if [ -z $confluent_api_key ]
then
    echo
    echo "(Error Message 003)  You did not include the proper use of the --confluent-api-key=<CONFLUENT_API_KEY> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --confluent-api-key=<CONFLUENT_API_KEY> --confluent-api-secret=<CONFLUENT_API_SECRET> --snowflake-warehouse=<SNOWFLAKE_WAREHOUSE> --service-account-user=<SERVICE_ACCOUNT_USER> --day-count=<DAY_COUNT> --auto-offset-reset=<earliest | latest> --number-of-api-keys-to-retain=<NUMBER_OF_API_KEYS_TO_RETAIN>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --confluent-api-secret argument was supplied
if [ -z $confluent_api_secret ]
then
    echo
    echo "(Error Message 004)  You did not include the proper use of the --confluent-api-secret=<CONFLUENT_API_SECRET> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --confluent-api-key=<CONFLUENT_API_KEY> --confluent-api-secret=<CONFLUENT_API_SECRET> --snowflake-warehouse=<SNOWFLAKE_WAREHOUSE> --service-account-user=<SERVICE_ACCOUNT_USER> --day-count=<DAY_COUNT> --auto-offset-reset=<earliest | latest> --number-of-api-keys-to-retain=<NUMBER_OF_API_KEYS_TO_RETAIN>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --snowflake-warehouse argument was supplied
if [ -z $snowflake_warehouse ]
then
    echo
    echo "(Error Message 005)  You did not include the proper use of the --snowflake-warehouse=<SNOWFLAKE_WAREHOUSE> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --confluent-api-key=<CONFLUENT_API_KEY> --confluent-api-secret=<CONFLUENT_API_SECRET> --snowflake-warehouse=<SNOWFLAKE_WAREHOUSE> --service-account-user=<SERVICE_ACCOUNT_USER> --day-count=<DAY_COUNT> --auto-offset-reset=<earliest | latest> --number-of-api-keys-to-retain=<NUMBER_OF_API_KEYS_TO_RETAIN>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --service-account-user argument was supplied
if [ -z $service_account_user ]
then
    echo
    echo "(Error Message 006)  You did not include the proper use of the --service-account-user=<SERVICE_ACCOUNT_USER> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --confluent-api-key=<CONFLUENT_API_KEY> --confluent-api-secret=<CONFLUENT_API_SECRET> --snowflake-warehouse=<SNOWFLAKE_WAREHOUSE> --service-account-user=<SERVICE_ACCOUNT_USER> --day-count=<DAY_COUNT> --auto-offset-reset=<earliest | latest> --number-of-api-keys-to-retain=<NUMBER_OF_API_KEYS_TO_RETAIN>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --day-count argument was supplied
if [ -z $day_count ] && [ create_action = true ]
then
    echo
    echo "(Error Message 007)  You did not include the proper use of the --day-count=<DAY_COUNT> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --confluent-api-key=<CONFLUENT_API_KEY> --confluent-api-secret=<CONFLUENT_API_SECRET> --snowflake-warehouse=<SNOWFLAKE_WAREHOUSE> --service-account-user=<SERVICE_ACCOUNT_USER> --day-count=<DAY_COUNT> --auto-offset-reset=<earliest | latest> --number-of-api-keys-to-retain=<NUMBER_OF_API_KEYS_TO_RETAIN>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --auto-offset-reset argument was supplied
if [ $auto_offset_reset_set = false ] && [ create_action = true ]
then
    echo
    echo "(Error Message 008)  You did not include the proper use of the --auto-offset-reset=<earliest | latest> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --confluent-api-key=<CONFLUENT_API_KEY> --confluent-api-secret=<CONFLUENT_API_SECRET> --snowflake-warehouse=<SNOWFLAKE_WAREHOUSE> --service-account-user=<SERVICE_ACCOUNT_USER> --day-count=<DAY_COUNT> --auto-offset-reset=<earliest | latest> --number-of-api-keys-to-retain=<NUMBER_OF_API_KEYS_TO_RETAIN>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --number-of-api-keys-to-retain argument was supplied
if [ -z $number_of_api_keys_to_retain ] && [ create_action = true ]
then
    echo
    echo "(Error Message 009)  You did not include the proper use of the --number-of-api-keys-to-retain=<NUMBER_OF_API_KEYS_TO_RETAIN> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` <create | delete> --profile=<SSO_PROFILE_NAME> --confluent-api-key=<CONFLUENT_API_KEY> --confluent-api-secret=<CONFLUENT_API_SECRET> --snowflake-warehouse=<SNOWFLAKE_WAREHOUSE> --service-account-user=<SERVICE_ACCOUNT_USER> --day-count=<DAY_COUNT> --auto-offset-reset=<earliest | latest> --number-of-api-keys-to-retain=<NUMBER_OF_API_KEYS_TO_RETAIN>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Retrieve from the AWS SSO account information to set the SSO AWS_ACCESS_KEY_ID, 
# AWS_ACCESS_SECRET_KEY, AWS_SESSION_TOKEN, AWS_REGION, and AWS_ACCOUNT_ID
# environmental variables
aws sso login $AWS_PROFILE
eval $(aws2-wrap $AWS_PROFILE --export)
export AWS_REGION=$(aws configure get sso_region $AWS_PROFILE)
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity $AWS_PROFILE --query "Account" --output text)

# Create terraform.tfvars file
if [ create_action = true ]
then
    printf "aws_account_id=\"${AWS_ACCOUNT_ID}\"\
    \naws_region=\"${AWS_REGION}\"\
    \naws_access_key_id=\"${AWS_ACCESS_KEY_ID}\"\
    \naws_secret_access_key=\"${AWS_SECRET_ACCESS_KEY}\"\
    \naws_session_token=\"${AWS_SESSION_TOKEN}\"\
    \nconfluent_api_key=\"${confluent_api_key}\"\
    \nconfluent_api_secret=\"${confluent_api_secret}\"\
    \nservice_account_user=\"${service_account_user}\"\
    \nsnowflake_warehouse=\"${snowflake_warehouse}\"\
    \nauto_offset_reset=\"${auto_offset_reset}\"\
    \nday_count=${day_count}\
    \nnumber_of_api_keys_to_retain=${number_of_api_keys_to_retain}" > terraform.tfvars
else
    printf "aws_account_id=\"${AWS_ACCOUNT_ID}\"\
    \naws_region=\"${AWS_REGION}\"\
    \naws_access_key_id=\"${AWS_ACCESS_KEY_ID}\"\
    \naws_secret_access_key=\"${AWS_SECRET_ACCESS_KEY}\"\
    \naws_session_token=\"${AWS_SESSION_TOKEN}\"\
    \nconfluent_api_key=\"${confluent_api_key}\"\
    \nconfluent_api_secret=\"${confluent_api_secret}\"\
    \nsnowflake_warehouse=\"${snowflake_warehouse}\"\
    \nservice_account_user=\"${service_account_user}\"" > terraform.tfvars
fi

# Initialize the Terraform configuration
terraform init

if [ "$create_action" = true ]
then
    # Create/Update the Terraform configuration
    terraform plan -var-file=terraform.tfvars
    terraform apply -var-file=terraform.tfvars
else
    # Destroy the Terraform configuration
    terraform destroy -var-file=terraform.tfvars

    # Delete the secrets created by the Terraform configurations
    service_account_user=$(echo $service_account_user | tr '[:upper:]' '[:lower:]')
    aws secretsmanager delete-secret $AWS_PROFILE --secret-id /confluent_cloud_resource/${service_account_user}/schema_registry_cluster/java_client --force-delete-without-recovery || true
    aws secretsmanager delete-secret $AWS_PROFILE --secret-id /confluent_cloud_resource/${service_account_user}/schema_registry_cluster/python_client --force-delete-without-recovery || true
    aws secretsmanager delete-secret $AWS_PROFILE --secret-id /confluent_cloud_resource/${service_account_user}/kafka_cluster/java_client --force-delete-without-recovery || true
    aws secretsmanager delete-secret $AWS_PROFILE --secret-id /confluent_cloud_resource/${service_account_user}/kafka_cluster/python_client --force-delete-without-recovery || true
    aws secretsmanager delete-secret $AWS_PROFILE --secret-id /confluent_cloud_resource/${service_account_user}/flink_compute_pool --force-delete-without-recovery || true
    aws secretsmanager delete-secret $AWS_PROFILE --secret-id /snowflake_resource/${service_account_user} --force-delete-without-recovery || true
    aws secretsmanager delete-secret $AWS_PROFILE --secret-id /snowflake_resource/${service_account_user}/rsa_private_key_pem_1 --force-delete-without-recovery || true
    aws secretsmanager delete-secret $AWS_PROFILE --secret-id /snowflake_resource/${service_account_user}/rsa_private_key_pem_2 --force-delete-without-recovery || true

    # Delete the AWS Glue Data Catalog Database, and all associated tables within database will also be deleted
    aws glue delete-database --name airlines || true

    # Delete the AWS S3 bucket and all objects within the bucket
    aws s3 rb s3://${service_account_user} --force || true
fi
