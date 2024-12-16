# Glue Role and Policy
resource "aws_iam_role" "glue_role" {
  name = "glue_service_role"

  assume_role_policy = jsonencode({
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Principal": {
          "Service": "glue.amazonaws.com"
        },
        "Action": "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_policy" "glue_s3_access_policy" {
  name = "GlueS3AccessPolicy"

  policy = jsonencode({
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "s3:GetObject",
          "s3:PutObject",
          "s3:ListBucket"
        ],
        "Resource": [
          aws_s3_bucket.iceberg_bucket.arn,
          "${aws_s3_bucket.iceberg_bucket.arn}/*"
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "glue_policy_attachment" {
  role       = aws_iam_role.glue_role.name
  policy_arn = aws_iam_policy.glue_s3_access_policy.arn
}

# Snowflake Role and Policy
resource "aws_iam_role" "snowflake_role" {
  name = "snowflake_role"

  assume_role_policy = jsonencode({
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": "sts:AssumeRole",
        "Principal": {
          "AWS": snowflake_storage_integration.aws_s3_integration.storage_aws_iam_user_arn
        }
        "Condition" : {
          "StringEquals" : {
            "sts:ExternalId" : snowflake_storage_integration.aws_s3_integration.storage_aws_external_id
          }
        }
      }
    ]
  })
}

resource "aws_iam_policy" "snowflake_s3_access_policy" {
  name = "SnowflakeS3AccessPolicy"

  policy = jsonencode({
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "s3:PutObject",
          "s3:GetObject",
          "s3:GetObjectVersion",
          "s3:DeleteObject",
          "s3:DeleteObjectVersion"
        ],
        "Resource": [          
          "${aws_s3_bucket.iceberg_bucket.arn}/warehouse/*"
        ]
      },
        {
            "Effect": "Allow",
            "Action": [
                "s3:ListBucket",
                "s3:GetBucketLocation"
            ],
            "Resource": aws_s3_bucket.iceberg_bucket.arn,
            "Condition": {
                "StringLike": {
                    "s3:prefix": [
                      "*"
                    ]
                }
            }
        }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "snowflake_policy_attachment" {
  role       = aws_iam_role.snowflake_role.name
  policy_arn = aws_iam_policy.snowflake_s3_access_policy.arn
}