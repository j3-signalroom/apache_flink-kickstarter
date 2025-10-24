output "s3_bucket_warehouse_name" {
  value = trimsuffix(local.s3_bucket_warehouse_name, "/")
}

output "glue_database_name" {
  value = local.catalog_namespace
}