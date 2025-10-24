output "s3_bucket_warehouse_name" {
  value = local.s3_bucket_warehouse_name
}

output "glue_database_name" {
  value = local.catalog_namespace
}