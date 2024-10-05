import boto3

# Initialize STS client
sts_client = boto3.client('sts')

# Get the caller identity
response = sts_client.get_caller_identity()
arn = response['Arn']

# Check if the caller identity contains a role
if "assumed-role" in arn:
    # Extract role name
    role_name = arn.split('/')[1]
    print(f"Role Name: {role_name}")
else:
    print(f"Not an assumed role: {arn}")
