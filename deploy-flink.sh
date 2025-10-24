#!/bin/bash

#
# *** Script Syntax ***
# ./deploy-flink.sh <on | off> --profile=<AWS_SSO_PROFILE_NAME>
#                              --chip=<amd64 | arm64>
#                              --flink-language=<python | java>
#
#

# Check required command (on or off) was supplied
case $1 in
  on)
    is_on=true;;
  off)
    is_on=false;;
  *)
    echo
    echo "(Error Message 001)  You did not specify one of the commands: <on | off>."
    echo
    echo "Usage:  Require ---> `basename $0` <on | off> --profile=<AWS_SSO_PROFILE_NAME> --chip=<amd64 | arm64> --flink-language=<python | java>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
    ;;
esac

# Get the arguments passed
AWS_S3_BUCKET=""
use_non_mac=true
chip_arg_provider=false
# Get the arguments passed by shift to remove the first word
# then iterate over the rest of the arguments
shift
for arg in "$@" # $@ sees arguments as separate words
do
    case $arg in
        *"--profile="*)
            AWS_PROFILE=$arg;;
        *"--aws-s3-bucket="*)
            arg_length=16
            aws_s3_bucket=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        --chip=amd64)
            chip_arg_provider=true
            use_non_mac=true;;
        --chip=arm64)
            chip_arg_provider=true
            use_non_mac=false;;
        --flink-language=python)
            language_arg_provider=true
            FLINK_LANGUAGE="python";;
        --flink-language=java)
            language_arg_provider=true
            FLINK_LANGUAGE="java";;
    esac
done

if [ $is_on = true ]
then
    # Check required --profile argument was supplied
    if [ -z $AWS_PROFILE ]
    then
        echo
        echo "(Error Message 002)  You did not include the proper use of the --profile=<AWS_SSO_PROFILE_NAME> argument in the call."
        echo
        echo "Usage:  Require ---> `basename $0` <on | off> --profile=<AWS_SSO_PROFILE_NAME> --chip=<amd64 | arm64> --flink-language=<python | java>"
        echo
        exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
    fi

    # Check required --chip argument was supplied
    if [ $chip_arg_provider = false ]
    then
        echo
        echo "(Error Message 003)  You did not include the proper use of the --chip=<amd64 | arm64> argument in the call."
        echo
        echo "Usage:  Require ---> `basename $0` <on | off> --profile=<AWS_SSO_PROFILE_NAME> --chip=<amd64 | arm64> --flink-language=<python | java>"
        echo
        exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
    fi

    # Check required --flink-language argument was supplied
    if [ $language_arg_provider = false ]
    then
        echo
        echo "(Error Message 004)  You did not include the proper use of the --flink-language=<python | java> argument in the call."
        echo
        echo "Usage:  Require ---> `basename $0` <on | off> --profile=<AWS_SSO_PROFILE_NAME> --chip=<amd64 | arm64> --flink-language=<python | java>"
        echo
        exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
    fi

    # Retrieve from the AWS SSO account information to set the SSO AWS_ACCESS_KEY_ID, 
    # AWS_ACCESS_SECRET_KEY, AWS_SESSION_TOKEN, and AWS_REGION environmental variables
    aws sso login $AWS_PROFILE
    eval $(aws2-wrap $AWS_PROFILE --export)
    export AWS_REGION=$(aws configure get sso_region $AWS_PROFILE)

    # Gets s3_bucket_warehouse_name of the AWS S3 Bucket created during the apply run
    s3_bucket_warehouse_name=$(terraform output -raw s3_bucket_warehouse_name)

    # Check if the s3_bucket_warehouse_name contains the word "warning", because the output
    # variable may not exist
    if echo "$s3_bucket_warehouse_name" | grep -iq "warning"
    then
       s3_bucket_warehouse_name="" 
    fi

    # Create and then pass the AWS environment variables to docker-compose
    printf "FLINK_LANGUAGE=${FLINK_LANGUAGE}\
    \nAWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}\
    \nAWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}\
    \nAWS_SESSION_TOKEN=${AWS_SESSION_TOKEN} \
    \nAWS_REGION=${AWS_REGION}\
    \nAWS_DEFAULT_REGION=${AWS_REGION}\
    \nAWS_PROFILE=${AWS_PROFILE}\
    \nAWS_S3_BUCKET=${s3_bucket_warehouse_name}" > .env

    # Deploy the Docker containers via docker-compose based on the platform
    if [ $use_non_mac = true ]
    then
        docker-compose -f linux-docker-compose.yml up -d 
    else
        docker-compose -f mac-docker-compose.yml up -d
    fi
else
    if [ $use_non_mac = true ]
    then
        docker-compose -f linux-docker-compose.yml down
    else
        docker-compose -f mac-docker-compose.yml down
    fi
fi