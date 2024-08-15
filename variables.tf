variable "confluent_cloud_api_key" {
  description = "Confluent Cloud API Key (also referred as Cloud API ID)."
  type        = string
}

variable "confluent_cloud_api_secret" {
  description = "Confluent Cloud API Secret."
  type        = string
  sensitive   = true
}

variable "aws_profile" {
    description = "The AWS Landing Zone Profile."
    type        = string
}

variable "aws_region" {
    description = "The AWS Region."
    type        = string
}

variable "aws_account_id" {
    description = "The AWS Account ID."
    type        = string
}

variable "aws_access_key" {
    description = "The AWS Access Key."
    type        = string
}

variable "aws_secret_key" {
    description = "The AWS Secret Key."
    type        = string
}

variable "aws_session_token" {
    description = "The AWS Session Token."
    type        = string
}
variable "day_count" {
    description = "How many day(s) should the API Key be rotated for."
    type        = number
    default     = 30
    
    validation {
        condition     = var.day_count >= 1
        error_message = "Rolling day count, `day_count`, must be greater than or equal to 1."
    }
}

variable "number_of_api_keys_to_retain" {
    description = "Specifies the number of API keys to create and retain.  Must be greater than or equal to 2 in order to maintain proper key rotation for your application(s)."
    type        = number
    default     = 2
    
    validation {
        condition     = var.number_of_api_keys_to_retain >= 2
        error_message = "Number of API keys to retain, `number_of_api_keys_to_retain`, must be greater than or equal to 2."
    }
}

variable "auto_offset_reset" {
    description = "Specifies the behavior of the consumer when there is no committed position (which occurs when the group is first initialized) or when an offset is out of range. You can choose either to reset the position to the 'earliest' offset or the 'latest' offset (the default)."
    type        = string
    default     = "latest"
    
    validation {
        condition     = contains(["earliest", "latest"], var.auto_offset_reset)
        error_message = "The auto_offset_reset must be either 'earliest' or 'latest'."
    }
}
