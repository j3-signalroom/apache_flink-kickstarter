#!/bin/bash

#
# *** Script Syntax ***
# scripts/run-flink-locally.sh --profile=<AWS_SSO_PROFILE_NAME>
#
# *** Example Call ***
# scripts/run-flink-locally.sh --profile=AdministratorAccess-0123456789
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
