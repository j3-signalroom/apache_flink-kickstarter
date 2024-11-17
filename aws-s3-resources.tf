
resource "aws_s3_bucket" "iceberg_bucket" {
  # Ensure the bucket name adheres to the S3 bucket naming conventions
  bucket = replace(local.secrets_insert, "_", "-")
}

resource "aws_s3_object" "warehouse" {
  bucket = aws_s3_bucket.iceberg_bucket.bucket
  key    = "warehouse/"
}
