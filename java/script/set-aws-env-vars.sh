#!/bin/bash

#
# *** Script Syntax ***
# script/set-aws-env-vars.sh --profile=<PROFILE_NAME>
#
# *** Example Call ***
# script/set-aws-env-vars.sh --profile=dev
#
# Check if arguments were supplied; otherwise exit script
if [ ! -n "$1" ]
then
    echo
    echo "Usage:  Require at least two arguments ---> `basename $0` --profile=<PROFILE_NAME>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Get the arguments passed
arg_count=0
for arg in "$@" # $@ sees arguments as separate words
do
    case $arg in
        *"--profile="*)
            arg_length=10
            environment_name=${arg:$arg_length:$(expr ${#arg} - $arg_length)}
            AWS_PROFILE=$arg;;
    esac
    let "arg_count+=1"
done

if [ "$arg_count" -lt "1" ]
then
    echo
    echo "Usage:  Require the ---> `basename $0` --profile=<PROFILE_NAME>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Get the SSO AWS_ACCESS_KEY_ID, AWS_ACCESS_SECRET_KEY, AWS_SESSION_TOKEN, AWS_REGION and AWS_ACCOUNT_ID, and
# set them as an environmental variables
aws sso login $AWS_PROFILE
eval $(aws2-wrap $AWS_PROFILE --export)
export AWS_REGION=$(aws configure get sso_region $AWS_PROFILE)
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query "Account" --output text)
