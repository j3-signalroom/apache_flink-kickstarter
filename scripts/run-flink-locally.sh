#!/bin/bash

#
# *** Script Syntax ***
# scripts/run-flink-locally.sh <up | down> --profile=<AWS_SSO_PROFILE_NAME>
#                                          --chip=<amd64 | armd64>
#                                          [--aws_s3_bucket=<AWS_S3_BUCKET_NAME>]
#
#

# Check required command (up or down) was supplied
case $1 in
  up)
    up_action=true;;
  down)
    up_action=false;;
  *)
    echo
    echo "(Error Message 001)  You did not specify one of the commands: <up | down>."
    echo
    echo "Usage:  Require ---> `basename $0` <up | down> --profile=<AWS_SSO_PROFILE_NAME> --chip=<amd64 | armd64> [--aws_s3_bucket=<AWS_S3_BUCKET_NAME>]"
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
        *"--aws_s3_bucket="*)
            arg_length=16
            AWS_S3_BUCKET=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        --chip=amd64)
            chip_arg_provider=true
            use_non_mac=true;;
        --chip=arm64)
            chip_arg_provider=true
            use_non_mac=false;;
    esac
done

# Check required --profile argument was supplied
if [ -z $AWS_PROFILE ]
then
    echo
    echo "(Error Message 002)  You did not include the proper use of the --profile=<AWS_SSO_PROFILE_NAME> argument in the call."
    echo
    echo "Usage:  Require ---> `basename $0` <up | down> --profile=<AWS_SSO_PROFILE_NAME> --chip=<amd64 | armd64> [--aws_s3_bucket=<AWS_S3_BUCKET_NAME>]"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --profile argument was supplied
if [ $chip_arg_provider = false ]
then
    echo
    echo "(Error Message 003)  You did not include the proper use of the --chip=<amd64 | armd64> argument in the call."
    echo
    echo "Usage:  Require ---> `basename $0` <up | down> --profile=<AWS_SSO_PROFILE_NAME> --chip=<amd64 | armd64> [--aws_s3_bucket=<AWS_S3_BUCKET_NAME>]"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Get the SSO AWS_ACCESS_KEY_ID, AWS_ACCESS_SECRET_KEY, AWS_SESSION_TOKEN, and AWS_REGION, and
# set them as an environmental variables
aws sso login $AWS_PROFILE
eval $(aws2-wrap $AWS_PROFILE --export)
export AWS_REGION=$(aws configure get sso_region $AWS_PROFILE)

# Create and then pass the AWS environment variables to docker-compose
if [ -z $AWS_S3_BUCKET ]
then
    printf "AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}\
    \nAWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}\
    \nAWS_SESSION_TOKEN=${AWS_SESSION_TOKEN} \
    \nAWS_REGION=${AWS_REGION}\
    \nAWS_DEFAULT_REGION=${AWS_REGION}" > .env
else
    printf "AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}\
    \nAWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}\
    \nAWS_SESSION_TOKEN=${AWS_SESSION_TOKEN} \
    \nAWS_REGION=${AWS_REGION}\
    \nAWS_DEFAULT_REGION=${AWS_REGION}\
    \nAWS_PROFILE=${AWS_PROFILE}\
    \nAWS_S3_BUCKET=${AWS_S3_BUCKET}" > .env
fi

# Run the Apache Flink cluster containers in the background (i.e., detach execution from the Termial window)
if [ $use_non_mac = false ]
then
    if [ $up_action = true ]
        docker-compose -f linux-docker-compose.yml up -d
    else
        docker-compose -f linux-docker-compose.yml down
    fi
else
    if [ $up_action = true ]
        docker-compose -f mac-docker-compose.yml up -d
    else
        docker-compose -f mac-docker-compose.yml down
    fi
fi