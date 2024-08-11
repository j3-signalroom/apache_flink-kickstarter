#!/bin/bash

#
# *** Script Syntax ***
# scripts/run-flink-locally.sh --profile=<AWS_SSO_PROFILE_NAME>
#
# *** Example Call ***
# scripts/run-flink-locally.sh --profile=AdministratorAccess-0123456789
#
# Check if arguments were supplied; otherwise exit script
if [ ! -n "$1" ]
then
    echo
    echo "(Error Message 001)  You did not include an argument in the call."
    echo
    echo "Usage:  Require at least two arguments ---> `basename $0` --profile=<AWS_SSO_PROFILE_NAME>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Get the arguments passed
arg_count=0
for arg in "$@" # $@ sees arguments as separate words
do
    case $arg in
        *"--profile="*)
            AWS_PROFILE=$arg;;
    esac
    let "arg_count+=1"
done

# Check required --profile argument was supplied
if [ -z $AWS_PROFILE ]
then
    echo
    echo "(Error Message 002)  You did not include the proper use of the --profile=<AWS_SSO_PROFILE_NAME> argument in the call."
    echo
    echo "Usage:  Require at least two arguments ---> `basename $0` --profile=<AWS_SSO_PROFILE_NAME>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Get the SSO AWS_ACCESS_KEY_ID, AWS_ACCESS_SECRET_KEY, AWS_SESSION_TOKEN, and AWS_REGION, and
# set them as an environmental variables
aws sso login $AWS_PROFILE
eval $(aws2-wrap $AWS_PROFILE --export)
export AWS_REGION=$(aws configure get sso_region $AWS_PROFILE)

# Force the destroy of all the AWS Secrets created by the Terraform configuration
aws secretsmanager delete-secret --secret-id /confluent_cloud_resource/schema_registry_cluster/java_client --force-delete-without-recovery
aws secretsmanager delete-secret --secret-id /confluent_cloud_resource/kafka_cluster/java_client --force-delete-without-recovery
