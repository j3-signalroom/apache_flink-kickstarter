
resource "random_uuid" "s3_bucket" {
}

resource "aws_s3_bucket" "iceberg_bucket" {
  # Ensure the bucket name adheres to the S3 bucket naming conventions and is globally unique.
  bucket        = "${replace(local.secrets_insert, "_", "-")}-${random_uuid.s3_bucket.id}"
  force_destroy = true
}

resource "aws_s3_object" "warehouse" {
  bucket = aws_s3_bucket.iceberg_bucket.bucket
  key    = "warehouse/"
}
