#!/bin/bash

#
# *** Script Syntax ***
# scripts/run-flink-locally.sh --profile=<PROFILE_NAME>
#
# *** Example Call ***
# scripts/run-flink-locally.sh --profile=dev
#
# Check if arguments were supplied; otherwise exit script
if [ ! -n "$1" ]
then
    echo
    echo "Usage:  Require at least two arguments ---> `basename $0` --profile=<PROFILE_NAME> --chip=<CHIP_NAME>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Get the arguments passed
aws_sso=false
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
        *"--aws-sso"*)
            aws_sso=true;;
    esac
    let "arg_count+=1"
done

if [ "$arg_count" -lt "2" ]
then
    echo
    echo "Usage:  Require at least two arguments ---> `basename $0` --profile=<PROFILE_NAME> --chip=<CHIP_NAME>"
    echo
    exit 85 # Common GNU/Linux Exit Code for 'Interrupted system call should be restarted'
fi

# Check if the --aws-sso flag was supplied
if [ "$aws_sso" = true ]
then
    # Get the SSO AWS_ACCESS_KEY_ID, AWS_ACCESS_SECRET_KEY, AWS_SESSION_TOKEN, and AWS_REGION, and
    # set them as an environmental variables
    aws sso login $AWS_PROFILE
    eval $(aws2-wrap $AWS_PROFILE --export)
    export AWS_REGION=$(aws configure get sso_region $AWS_PROFILE)
    export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query "Account" --output text)

    # Create and then pass the AWS environment variables to docker-compose
    printf "AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}\
    \nAWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}\
    \nAWS_REGION=${AWS_REGION}\
    \nAWS_DEFAULT_REGION=${AWS_REGION}\
    \nFLINK_DOCKER_IMAGE=${flink_docker_image} > .env
else
    # Create and then pass the AWS environment variables to docker-compose
    printf "FLINK_DOCKER_IMAGE=${flink_docker_image} > .env
fi

docker-compose up 