#!/bin/bash

# Get caller identity to get the ARN
caller_identity=$(aws sts get-caller-identity --query "Arn" --output text)

# Check if the caller identity contains a role
if [[ $caller_identity == *":assumed-role/"* ]]; then
    # Extract role name from ARN
    role_name=$(echo $caller_identity | awk -F'/' '{print $2}')
    echo "Role Name: $role_name"
else
    echo "Not an assumed role: $caller_identity"
fi
