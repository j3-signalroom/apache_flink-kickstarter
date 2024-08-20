#!/bin/bash

#
# *** Script Syntax ***
# scripts/run-terraform-locally.sh --environment=<ENVIRONMENT_NAME> --profile=<SSO_PROFILE_NAME> --confluent_cloud_api_key=<CONFLUENT_CLOUD_API_KEY> --confluent_cloud_api_secret=<CONFLUENT_CLOUD_API_SECRETS> --action=<create | destroy> --snowflake_account_id=<SNOWFLAKE_ACCOUNT_ID> --snowflake_login_url=<SNOWFLAKE_LOGIN_URL> --snowflake_username=<SNOWFLAKE_USERNAME> --snowflake_password=<SNOWFLAKE_PASSWORD>
#
#

# Check if arguments were supplied; otherwise exit script
if [ ! -n "$1" ]
then
    echo
    echo "(Error Message 001)  You did not include all four arguments in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` --environment=<ENVIRONMENT_NAME> --profile=<SSO_PROFILE_NAME> --confluent_cloud_api_key=<CONFLUENT_CLOUD_API_KEY> --confluent_cloud_api_secret=<CONFLUENT_CLOUD_API_SECRETS> --action=<create | destroy> --snowflake_account_id=<SNOWFLAKE_ACCOUNT_ID> --snowflake_login_url=<SNOWFLAKE_LOGIN_URL> --snowflake_username=<SNOWFLAKE_USERNAME> --snowflake_password=<SNOWFLAKE_PASSWORD>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Get the arguments passed
arg_count=0
action_argument_supplied=false
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
        *"--action=create"*)
            action_argument_supplied=true
            create_action=true;;
        *"--action=destroy"*)
            action_argument_supplied=true
            create_action=false;;
        *"--snowflake_account_id="*)
            arg_length=23
            snowflake_account_id=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--snowflake_login_url="*)
            arg_length=22
            snowflake_login_url=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--snowflake_username="*)
            arg_length=21
            snowflake_username=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
        *"--snowflake_password="*)
            arg_length=21
            snowflake_password=${arg:$arg_length:$(expr ${#arg} - $arg_length)};;
    esac
    let "arg_count+=1"
done

# Check required --environment argument was supplied
if [ -z $environment_name ]
then
    echo
    echo "(Error Message 002)  You did not include the proper use of the --environment=<ENVIRONMENT_NAME> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` --environment=<ENVIRONMENT_NAME> --profile=<SSO_PROFILE_NAME> --confluent_cloud_api_key=<CONFLUENT_CLOUD_API_KEY> --confluent_cloud_api_secret=<CONFLUENT_CLOUD_API_SECRETS> --action=<create | destroy> --snowflake_account_id=<SNOWFLAKE_ACCOUNT_ID> --snowflake_login_url=<SNOWFLAKE_LOGIN_URL> --snowflake_username=<SNOWFLAKE_USERNAME> --snowflake_password=<SNOWFLAKE_PASSWORD>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --profile argument was supplied
if [ -z $AWS_PROFILE ]
then
    echo
    echo "(Error Message 003)  You did not include the proper use of the --profile=<AWS_SSO_SSO_PROFILE_NAME> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` --environment=<ENVIRONMENT_NAME> --profile=<SSO_PROFILE_NAME> --confluent_cloud_api_key=<CONFLUENT_CLOUD_API_KEY> --confluent_cloud_api_secret=<CONFLUENT_CLOUD_API_SECRETS> --action=<create | destroy> --snowflake_account_id=<SNOWFLAKE_ACCOUNT_ID> --snowflake_login_url=<SNOWFLAKE_LOGIN_URL> --snowflake_username=<SNOWFLAKE_USERNAME> --snowflake_password=<SNOWFLAKE_PASSWORD>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --confluent_cloud_api_key argument was supplied
if [ -z $confluent_cloud_api_key ]
then
    echo
    echo "(Error Message 004)  You did not include the proper use of the --confluent_cloud_api_key=<CONFLUENT_CLOUD_API_KEY> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` --environment=<ENVIRONMENT_NAME> --profile=<SSO_PROFILE_NAME> --confluent_cloud_api_key=<CONFLUENT_CLOUD_API_KEY> --confluent_cloud_api_secret=<CONFLUENT_CLOUD_API_SECRETS> --action=<create | destroy> --snowflake_account_id=<SNOWFLAKE_ACCOUNT_ID> --snowflake_login_url=<SNOWFLAKE_LOGIN_URL> --snowflake_username=<SNOWFLAKE_USERNAME> --snowflake_password=<SNOWFLAKE_PASSWORD>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --confluent_cloud_api_secret argument was supplied
if [ -z $confluent_cloud_api_secret ]
then
    echo
    echo "(Error Message 005)  You did not include the proper use of the --confluent_cloud_api_secret=<CONFLUENT_CLOUD_API_SECRETS> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` --environment=<ENVIRONMENT_NAME> --profile=<SSO_PROFILE_NAME> --confluent_cloud_api_key=<CONFLUENT_CLOUD_API_KEY> --confluent_cloud_api_secret=<CONFLUENT_CLOUD_API_SECRETS> --action=<create | destroy> --snowflake_account_id=<SNOWFLAKE_ACCOUNT_ID> --snowflake_login_url=<SNOWFLAKE_LOGIN_URL> --snowflake_username=<SNOWFLAKE_USERNAME> --snowflake_password=<SNOWFLAKE_PASSWORD>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --action argument was supplied
if [ "$action_argument_supplied" = false ]
then
    echo
    echo "(Error Message 006)  You did not include the proper use of the --action=<create | destroy> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` --environment=<ENVIRONMENT_NAME> --profile=<SSO_PROFILE_NAME> --confluent_cloud_api_key=<CONFLUENT_CLOUD_API_KEY> --confluent_cloud_api_secret=<CONFLUENT_CLOUD_API_SECRETS> --action=<create | destroy> --snowflake_account_id=<SNOWFLAKE_ACCOUNT_ID> --snowflake_login_url=<SNOWFLAKE_LOGIN_URL> --snowflake_username=<SNOWFLAKE_USERNAME> --snowflake_password=<SNOWFLAKE_PASSWORD>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --snowflake_account_id argument was supplied
if [ -z $snowflake_account_id ]
then
    echo
    echo "(Error Message 007)  You did not include the proper use of the --snowflake_account_id=<SNOWFLAKE_ACCOUNT_ID> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` --environment=<ENVIRONMENT_NAME> --profile=<SSO_PROFILE_NAME> --confluent_cloud_api_key=<CONFLUENT_CLOUD_API_KEY> --confluent_cloud_api_secret=<CONFLUENT_CLOUD_API_SECRETS> --action=<create | destroy> --snowflake_account_id=<SNOWFLAKE_ACCOUNT_ID> --snowflake_login_url=<SNOWFLAKE_LOGIN_URL> --snowflake_username=<SNOWFLAKE_USERNAME> --snowflake_password=<SNOWFLAKE_PASSWORD>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --snowflake_login_url argument was supplied
if [ -z $snowflake_login_url ]
then
    echo
    echo "(Error Message 008)  You did not include the proper use of the --snowflake_login_url=<SNOWFLAKE_LOGIN_URL> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` --environment=<ENVIRONMENT_NAME> --profile=<SSO_PROFILE_NAME> --confluent_cloud_api_key=<CONFLUENT_CLOUD_API_KEY> --confluent_cloud_api_secret=<CONFLUENT_CLOUD_API_SECRETS> --action=<create | destroy> --snowflake_account_id=<SNOWFLAKE_ACCOUNT_ID> --snowflake_login_url=<SNOWFLAKE_LOGIN_URL> --snowflake_username=<SNOWFLAKE_USERNAME> --snowflake_password=<SNOWFLAKE_PASSWORD>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --snowflake_username argument was supplied
if [ -z $snowflake_username ]
then
    echo
    echo "(Error Message 009)  You did not include the proper use of the --snowflake_username=<SNOWFLAKE_USERNAME> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` --environment=<ENVIRONMENT_NAME> --profile=<SSO_PROFILE_NAME> --confluent_cloud_api_key=<CONFLUENT_CLOUD_API_KEY> --confluent_cloud_api_secret=<CONFLUENT_CLOUD_API_SECRETS> --action=<create | destroy> --snowflake_account_id=<SNOWFLAKE_ACCOUNT_ID> --snowflake_login_url=<SNOWFLAKE_LOGIN_URL> --snowflake_username=<SNOWFLAKE_USERNAME> --snowflake_password=<SNOWFLAKE_PASSWORD>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --snowflake_password argument was supplied
if [ -z $snowflake_password ]
then
    echo
    echo "(Error Message 010)  You did not include the proper use of the --snowflake_password=<SNOWFLAKE_PASSWORD> argument in the call."
    echo
    echo "Usage:  Require all four arguments ---> `basename $0` --environment=<ENVIRONMENT_NAME> --profile=<SSO_PROFILE_NAME> --confluent_cloud_api_key=<CONFLUENT_CLOUD_API_KEY> --confluent_cloud_api_secret=<CONFLUENT_CLOUD_API_SECRETS> --action=<create | destroy> --snowflake_account_id=<SNOWFLAKE_ACCOUNT_ID> --snowflake_login_url=<SNOWFLAKE_LOGIN_URL> --snowflake_username=<SNOWFLAKE_USERNAME> --snowflake_password=<SNOWFLAKE_PASSWORD>"
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
\nsnowflake_account_id=\"${snowflake_account_id}\"\
\nsnowflake_login_url=\"${snowflake_login_url}\"\
\nsnowflake_username=\"${snowflake_username}\"\
\nsnowflake_password=\"${snowflake_password}\"\
\nnumber_of_api_keys_to_retain = 2\
\nday_count=30\
\nauto_offset_reset=\"earliest\"" > terraform.tfvars

terraform init

if [ "$create_action" = true ]
then
    terraform plan -var-file=terraform.tfvars
    terraform apply -var-file=terraform.tfvars
else
    terraform destroy -var-file=terraform.tfvars
fi
