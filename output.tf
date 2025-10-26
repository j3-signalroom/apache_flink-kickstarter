output "s3_bucket_warehouse_name" {
  value = trimsuffix(local.s3_bucket_warehouse_name, "/")
}

output "glue_database_name" {
  value = local.catalog_namespace
}

output "service_account_user" {
  value = lower(var.service_account_user)
}