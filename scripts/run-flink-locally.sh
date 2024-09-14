#!/bin/bash

#
# *** Script Syntax ***
# scripts/run-flink-locally.sh --profile=<AWS_SSO_PROFILE_NAME> [--aws_s3_bucket=<AWS_S3_BUCKET_NAME>]
#
#

# Get the arguments passed
AWS_S3_BUCKET=""
for arg in "$@" # $@ sees arguments as separate words
do
    case $arg in
        *"--profile="*)
            AWS_PROFILE=$arg;;
        *"--aws_s3_bucket="*)
            arg_length=16
            AWS_S3_BUCKET=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
    esac
done

# Check required --profile argument was supplied
if [ -z $AWS_PROFILE ]
then
    echo
    echo "(Error Message 001)  You did not include the proper use of the --profile=<AWS_SSO_PROFILE_NAME> argument in the call."
    echo
    echo "Usage:  Require ---> `basename $0` --profile=<AWS_SSO_PROFILE_NAME> [--aws_s3_bucket=<AWS_S3_BUCKET_NAME>]"
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
docker-compose up -d