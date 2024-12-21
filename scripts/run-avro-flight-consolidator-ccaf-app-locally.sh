#!/bin/bash

#
# *** Script Syntax ***
# scripts/run-avro-flight-consolidator-ccaf-app-locally.sh --profile=<AWS_SSO_PROFILE_NAME>
#                                                          --service-account-user=<SERVICE_ACCOUNT_USER>
#                           
#

for arg in "$@" # $@ sees arguments as separate words
do
    case $arg in
        *"--profile="*)
            AWS_PROFILE=$arg;;
        *"--service-account-user="*)
            arg_length=23
            SERVICE_ACCOUNT_USER=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
    esac
done

# Check required --profile argument was supplied
if [ -z $AWS_PROFILE ]
then
    echo
    echo "(Error Message 001)  You did not include the proper use of the --profile=<AWS_SSO_PROFILE_NAME> argument in the call."
    echo
    echo "Usage:  Require ---> `basename $0` --profile=<AWS_SSO_PROFILE_NAME> --service-account-user=<SERVICE_ACCOUNT_USER>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --profile argument was supplied
if [ -z $SERVICE_ACCOUNT_USER ]
then
    echo
    echo "(Error Message 002)  You did not include the proper use of the --service-account-user=<SERVICE_ACCOUNT_USER> argument in the call."
    echo
    echo "Usage:  Require ---> `basename $0` --profile=<AWS_SSO_PROFILE_NAME> --service-account-user=<SERVICE_ACCOUNT_USER>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Retrieve from the AWS SSO account information to set the SSO AWS_ACCESS_KEY_ID, 
# AWS_ACCESS_SECRET_KEY, AWS_SESSION_TOKEN, and AWS_REGION environmental variables
aws sso login $AWS_PROFILE
eval $(aws2-wrap $AWS_PROFILE --export)
export AWS_REGION=$(aws configure get sso_region $AWS_PROFILE)

cd python_ccaf
poetry shell
poetry run avro_flight_consolidator_ccaf_app --service-account-user $SERVICE_ACCOUNT_USER --aws-region $AWS_REGION
