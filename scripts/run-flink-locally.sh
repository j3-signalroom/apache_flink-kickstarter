#!/bin/bash

#
# *** Script Syntax ***
# scripts/run-flink-locally.sh --profile=<AWS_SSO_PROFILE_NAME> --chip=<amd64 | arm64>
#
# *** Example Call ***
# scripts/run-flink-locally.sh --profile=AdministratorAccess-0123456789 --chip=arm64
#
# Check if arguments were supplied; otherwise exit script
if [ ! -n "$1" ]
then
    echo
    echo "(Error Message 001)  You did not include any arguments in the call."
    echo
    echo "Usage:  Require at least two arguments ---> `basename $0` --profile=<AWS_SSO_PROFILE_NAME> --chip=<amd64 | arm64>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Get the arguments passed
flink_docker_image=''
arg_count=0
for arg in "$@" # $@ sees arguments as separate words
do
    case $arg in
        *"--profile="*)
            AWS_PROFILE=$arg;;
        *"--chip=amd64"*)
            flink_docker_image='j3signalroom/linux_flink-with_hadoop_iceberg:latest';;
        *"--chip=arm64"*)
            flink_docker_image='j3signalroom/mac_flink-with_hadoop_iceberg:latest';;
    esac
    let "arg_count+=1"
done

# Check required --profile argument was supplied
if [ -z $AWS_PROFILE ]
then
    echo
    echo "(Error Message 002)  You did not include the proper use of the --profile=<AWS_SSO_PROFILE_NAME> argument in the call."
    echo
    echo "Usage:  Require at least two arguments ---> `basename $0` --profile=<AWS_SSO_PROFILE_NAME> --chip=<amd64 | arm64>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check all required arguments were supplied
if [ "$arg_count" -lt "2" ]
then
    echo
    echo "(Error Message 003)  You did not include all the required arguments in the call."
    echo
    echo "Usage:  Require at least two arguments ---> `basename $0` --profile=<AWS_SSO_PROFILE_NAME> --chip=<amd64 | arm64>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check required --chip argument was supplied
if [ -z $flink_docker_image ]
then
    echo
    echo "(Error Message 004)  You did not include the property use of the --chip=<amd64 | arm64> argument in the call."
    echo
    echo "Usage:  Require at least two arguments ---> `basename $0` --profile=<AWS_SSO_PROFILE_NAME> --chip=<amd64 | arm64>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Get the SSO AWS_ACCESS_KEY_ID, AWS_ACCESS_SECRET_KEY, AWS_SESSION_TOKEN, and AWS_REGION, and
# set them as an environmental variables
aws sso login $AWS_PROFILE
eval $(aws2-wrap $AWS_PROFILE --export)
export AWS_REGION=$(aws configure get sso_region $AWS_PROFILE)

# Create and then pass the AWS environment variables to docker-compose
printf "AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}\
\nAWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}\
\nAWS_SESSION_TOKEN=${AWS_SESSION_TOKEN} \
\nAWS_REGION=${AWS_REGION}\
\nAWS_DEFAULT_REGION=${AWS_REGION}\
\nFLINK_DOCKER_IMAGE=${flink_docker_image}" > .env

# Run the Apache Flink cluster containers in the background (i.e., detach execution from the Termial window)
docker-compose up -d