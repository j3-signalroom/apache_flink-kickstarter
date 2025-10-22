resource "aws_iam_role" "snowflake_s3_glue_role" {
  name               = local.snowflake_aws_s3_glue_role_name
  description        = "IAM role for Snowflake S3 and Glue access."

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          AWS = [
            jsondecode(local.external_volume_properties["STORAGE_LOCATION_1"])["STORAGE_AWS_IAM_USER_ARN"]
          ]
        }
        Action = "sts:AssumeRole"
        Condition = {
          StringEquals = {
            "sts:ExternalId" = jsondecode(local.external_volume_properties["STORAGE_LOCATION_1"])["STORAGE_AWS_EXTERNAL_ID"]
          }
        }
      },
      {
        Effect = "Allow"
        Principal = {
          AWS = local.catalog_integration_query_result_map["GLUE_AWS_IAM_USER_ARN"]
        }
        Action = "sts:AssumeRole"
        Condition = {
          StringEquals = {
            "sts:ExternalId" = local.catalog_integration_query_result_map["GLUE_AWS_EXTERNAL_ID"]
          }
        }
      },
      {
        Effect = "Allow",
        Principal = {
          Service = "glue.amazonaws.com"
        },
        Action = "sts:AssumeRole"
      }
    ]
  })

  depends_on = [ 
    snowflake_external_volume.tableflow_kickstarter_volume,
    snowflake_execute.catalog_integration,
    snowflake_grant_privileges_to_account_role.integration_usage
  ]
}

resource "aws_iam_policy" "snowflake_s3_glue_role_access_policy" {
  name   = "${local.snowflake_aws_s3_glue_role_name}_access_policy"

  policy = jsonencode(({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "s3:PutObject",
          "s3:PutObjectTagging",
          "s3:GetObject",
          "s3:GetObjectVersion",
          "s3:DeleteObject",
          "s3:DeleteObjectVersion",
          "s3:AbortMultipartUpload",
          "s3:ListMultipartUploadParts"
        ],
        Resource = "arn:aws:s3:::${aws_s3_bucket.iceberg_bucket.bucket}/*"
      },
      {
        Effect = "Allow",
        Action = [
          "s3:GetBucketLocation",
          "s3:ListBucketMultipartUploads",
          "s3:ListBucket"
        ],
        Resource = "arn:aws:s3:::${aws_s3_bucket.iceberg_bucket.bucket}",
        Condition = {
          StringLike = {
            "s3:prefix" = ["*"]
          }
        }
      },
      {
        Effect = "Allow",
        Action = [
          "glue:GetCatalog",
          "glue:GetDatabase",
          "glue:GetDatabases",
          "glue:GetTable",
          "glue:GetTables"
        ],
        Resource = [
          "arn:aws:glue:${var.aws_region}:${data.aws_caller_identity.current.account_id}:table/*/*",
          "arn:aws:glue:${var.aws_region}:${data.aws_caller_identity.current.account_id}:catalog",
          "arn:aws:glue:${var.aws_region}:${data.aws_caller_identity.current.account_id}:database/*"
        ]
      }
    ]
  }))

  depends_on = [
    aws_iam_role.snowflake_s3_glue_role
  ]
}

resource "aws_iam_role_policy_attachment" "snowflake_s3_glue_policy_attachment" {
  role       = aws_iam_role.snowflake_s3_glue_role.name
  policy_arn = aws_iam_policy.snowflake_s3_glue_role_access_policy.arn
}

# Update the IAM role for Snowflake S3 and Glue access to include the local IAM role
# to manage the cross-account access.
resource "aws_iam_role" "update_snowflake_s3_glue_role" {
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          AWS = [
            jsondecode(local.external_volume_properties["STORAGE_LOCATION_1"])["STORAGE_AWS_IAM_USER_ARN"],
            local.snowflake_aws_s3_glue_role_arn
          ]
        }
        Action = "sts:AssumeRole"
        Condition = {
          StringEquals = {
            "sts:ExternalId" = jsondecode(local.external_volume_properties["STORAGE_LOCATION_1"])["STORAGE_AWS_EXTERNAL_ID"]
          }
        }
      },
      {
        Effect = "Allow"
        Principal = {
          AWS = local.catalog_integration_query_result_map["GLUE_AWS_IAM_USER_ARN"]
        }
        Action = "sts:AssumeRole"
        Condition = {
          StringEquals = {
            "sts:ExternalId" = local.catalog_integration_query_result_map["GLUE_AWS_EXTERNAL_ID"]
          }
        }
      },
      {
        Effect = "Allow",
        Principal = {
          Service = "glue.amazonaws.com"
        },
        Action = "sts:AssumeRole"
      }
    ]
  })

  depends_on = [ 
    aws_iam_role_policy_attachment.snowflake_s3_glue_policy_attachment
  ]
}
