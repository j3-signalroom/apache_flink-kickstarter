#!/bin/bash

#
# *** Script Syntax ***
# scripts/run-ccaf-docker-locally.sh --profile=<AWS_SSO_PROFILE_NAME>
#                                    --catalog-name=<CATALOG_NAME>
#                                    --database-name=<DATABASE_NAME>
#

for arg in "$@" # $@ sees arguments as separate words
do
    case $arg in
        *"--profile="*)
            AWS_PROFILE=$arg;;
        *"--catalog-name="*)
            arg_length=15
            CATALOG_NAME=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--database-name="*)
            arg_length=16
            DATABASE_NAME=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
    esac
done

# Check required --profile argument was supplied
if [ -z $AWS_PROFILE ]
then
    echo
    echo "(Error Message 001)  You did not include the proper use of the --profile=<AWS_SSO_PROFILE_NAME> argument in the call."
    echo
    echo "Usage:  Require ---> `basename $0` --profile=<AWS_SSO_PROFILE_NAME> --catalog-name=<CATALOG_NAME> --database-name=<DATABASE_NAME>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --catalog-name argument was supplied
if [ -z $CATALOG_NAME ]
then
    echo
    echo "(Error Message 002)  You did not include the proper use of the --catalog-name=<CATALOG_NAME> argument in the call."
    echo
    echo "Usage:  Require ---> `basename $0` --profile=<AWS_SSO_PROFILE_NAME> --catalog-name=<CATALOG_NAME> --database-name=<DATABASE_NAME>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --database-name argument was supplied
if [ -z $DATABASE_NAME ]
then
    echo
    echo "(Error Message 003)  You did not include the proper use of the --database-name=<DATABASE_NAME> argument in the call."
    echo
    echo "Usage:  Require ---> `basename $0` --profile=<AWS_SSO_PROFILE_NAME> --catalog-name=<CATALOG_NAME> --database-name=<DATABASE_NAME>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Retrieve from the AWS SSO account information to set the SSO AWS_ACCESS_KEY_ID, 
# AWS_ACCESS_SECRET_KEY, AWS_SESSION_TOKEN, and AWS_REGION environmental variables
aws sso login $AWS_PROFILE
eval $(aws2-wrap $AWS_PROFILE --export)
export AWS_REGION=$(aws configure get sso_region $AWS_PROFILE)

# Build local docker container image showing detail progress of build
docker build \
       -t ccaf_app \
       --build-arg AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID \
       --build-arg AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY \
       --build-arg AWS_SESSION_TOKEN=$AWS_SESSION_TOKEN \
       --build-arg AWS_REGION=$AWS_REGION \
       --build-arg CATALOG_NAME=$CATALOG_NAME \
       --build-arg DATABASE_NAME=$DATABASE_NAME \
       .

# Run the image
docker run ccaf_app