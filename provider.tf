provider "confluent" {
  cloud_api_key    = var.confluent_cloud_api_key
  cloud_api_secret = var.confluent_cloud_api_secret
}

provider "aws" {
    region     = var.aws_region
    #access_key = var.aws_access_key_id
    #secret_key = var.aws_secret_access_key
    #token      = var.aws_session_token
}
