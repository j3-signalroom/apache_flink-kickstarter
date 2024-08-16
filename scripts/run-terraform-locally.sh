#!/bin/bash

#
# *** Script Syntax ***
# scripts/run-terraform-locally.sh --profile=<AWS_SSO_PROFILE_NAME>
#
# *** Example Call ***
# scripts/run-terraform-locally.sh --profile=AdministratorAccess-0123456789
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
        *"--environment="*)
            arg_length=14
            environment_name=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--confluent_cloud_api_key="*)
            arg_length=26
            confluent_cloud_api_key=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--confluent_cloud_api_secret="*)
            arg_length=29
            confluent_cloud_api_secret=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
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
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query "Account" --output text)

# Create terraform.tfvars file
printf "confluent_cloud_api_key=\"${confluent_cloud_api_key}\"\
\nconfluent_cloud_api_secret=\"${confluent_cloud_api_secret}\"\
\naws_account_id=\"${AWS_ACCOUNT_ID}\"\
\naws_profile=\"${environment_name}\"\
\naws_region=\"${AWS_REGION}\"\
\naws_access_key_id=\"${AWS_ACCESS_KEY_ID}\"\
\naws_secret_access_key=\"${AWS_SECRET_ACCESS_KEY}\"\
\naws_session_token=\"${AWS_SESSION_TOKEN}\"\
\nnumber_of_api_keys_to_retain = 2\
\nday_count=30\
\nauto_offset_reset=\"earliest\"" > terraform.tfvars

terraform init
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars