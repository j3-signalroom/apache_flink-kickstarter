# Create the Snowflake user RSA keys pairs
module "snowflake_service_user_rsa_key_pairs_rotation" {   
  source  = "github.com/j3-signalroom/iac-snowflake-service_user-rsa_key_pairs_rotation-tf_module"

  # Required Input(s)
  aws_region                    = var.aws_region
  snowflake_account_identifier  = local.snowflake_account_identifier
  snowflake_service_user        = local.secrets_insert
  secrets_path                  = "/snowflake_resource/${local.secrets_insert}"
  lambda_function_name          = local.secrets_insert

  # Optional Input(s)
  day_count                     = var.day_count
  aws_lambda_memory_size        = var.aws_lambda_memory_size
  aws_lambda_timeout            = var.aws_lambda_timeout
  aws_log_retention_in_days     = var.aws_log_retention_in_days
}

# Emits CREATE USER <user_name> TYPE=SERVICE DEFAULT_WAREHOUSE = <warehouse_name> DEFAULT_ROLE = <system_admin_role> DEFAULT_NAMESPACE = <database_name>.<schema_name> RSA_PUBLIC_KEY = <rsa_public_key> RSA_PUBLIC_KEY_2 = NULL;
resource "snowflake_service_user" "service_user" {
  provider          = snowflake.security_admin
  name              = upper(var.service_account_user)
  default_warehouse = snowflake_warehouse.apache_flink.name
  default_role      = snowflake_account_role.security_admin_role.name
  default_namespace = "${snowflake_database.apache_flink.name}.${snowflake_schema.apache_flink_schema.name}"

  # Setting the attributes to `null`, effectively unsets the attribute
  # Refer to this link `https://docs.snowflake.com/en/user-guide/key-pair-auth#configuring-key-pair-rotation`
  # for more information
  rsa_public_key    = module.snowflake_service_user_rsa_key_pairs_rotation.active_key_number == 1 ? module.snowflake_service_user_rsa_key_pairs_rotation.snowflake_rsa_public_key_1_pem : null
  rsa_public_key_2  = module.snowflake_service_user_rsa_key_pairs_rotation.active_key_number == 2 ? module.snowflake_service_user_rsa_key_pairs_rotation.snowflake_rsa_public_key_2_pem : null

    depends_on = [ 
    module.snowflake_service_user_rsa_key_pairs_rotation
  ]
}

# Emits CREATE ROLE <security_admin_role> COMMENT = 'Security Admin role for <user_name>';
resource "snowflake_account_role" "security_admin_role" {
  provider = snowflake.security_admin
  name     = local.security_admin_role
  comment  = "Security Admin role for ${local.user_name}"
}

# Emits GRANT ROLE <security_admin_role> TO USER <user_name>;
resource "snowflake_grant_account_role" "user_security_admin" {
  provider  = snowflake.security_admin
  role_name = snowflake_account_role.security_admin_role.name
  user_name = snowflake_service_user.service_user.name
  
  depends_on = [ 
    snowflake_service_user.service_user,
    snowflake_account_role.security_admin_role 
  ]
}

# Emits CREATE ROLE <system_admin_role> COMMENT = 'System Admin role for <user_name>';
resource "snowflake_account_role" "system_admin_role" {
  provider = snowflake.security_admin
  name     = local.system_admin_role
  comment  = "System Admin role for ${local.user_name}"
}

# Emits GRANT ROLE <user_system_admin> TO USER <user_name>;
resource "snowflake_grant_account_role" "user_system_admin" {
  provider  = snowflake
  role_name = snowflake_account_role.system_admin_role.name
  user_name = snowflake_service_user.service_user.name
  
  depends_on = [ 
    snowflake_service_user.service_user,
    snowflake_account_role.system_admin_role 
  ]
}

# Emits GRANT ALL PRIVILEGES ON USER <user_name> TO ROLE <security_admin_role>;
resource "snowflake_grant_privileges_to_account_role" "user_all_privileges" {
  provider          = snowflake.security_admin
  privileges        = ["ALL PRIVILEGES"]
  account_role_name = snowflake_account_role.security_admin_role.name  
  on_account_object {
    object_type = "USER"
    object_name = local.user_name
  }

  depends_on = [ 
    snowflake_grant_account_role.user_security_admin 
  ]
}

# Emits GRANT USAGE ON WAREHOUSE <warehouse_name> TO ROLE <system_admin_role>;
resource "snowflake_grant_privileges_to_account_role" "warehouse_usage" {
  provider          = snowflake.account_admin
  privileges        = ["USAGE"]
  account_role_name = snowflake_account_role.system_admin_role.name
  on_account_object {
    object_type = "WAREHOUSE"
    object_name = local.warehouse_name
  }

  depends_on = [ 
    snowflake_account_role.system_admin_role,
    snowflake_grant_account_role.user_system_admin,
    snowflake_warehouse.warehouse
  ]
}

# Emits GRANT USAGE ON DATABASE <database_name> TO ROLE <system_admin_role>;
resource "snowflake_grant_privileges_to_account_role" "database_usage" {
  provider          = snowflake.account_admin
  privileges        = ["USAGE"]
  account_role_name = snowflake_account_role.system_admin_role.name
  on_account_object {
    object_type = "DATABASE"
    object_name = local.database_name
  }

  depends_on = [ 
    snowflake_account_role.system_admin_role,
    snowflake_grant_account_role.user_system_admin,
    snowflake_database.database
  ]
}

# Emits GRANT USAGE ON EXTERNAL VOLUME <volume_name> TO ROLE <system_admin_role>;
resource "snowflake_grant_privileges_to_account_role" "external_volume_usage" {
  provider          = snowflake.account_admin
  privileges        = ["USAGE"]
  account_role_name = snowflake_account_role.system_admin_role.name
  on_account_object {
      object_type = "EXTERNAL VOLUME"
      object_name = local.volume_name
  }

  depends_on = [
    snowflake_account_role.system_admin_role,
    snowflake_grant_account_role.user_system_admin,
    snowflake_external_volume.tableflow_kickstarter_volume
  ]
}

# Emits GRANT USAGE ON INTEGRATION <integration_name> TO ROLE <system_admin_role>;
resource "snowflake_grant_privileges_to_account_role" "integration_usage" {
  provider          = snowflake.account_admin
  privileges        = ["USAGE"]
  account_role_name = snowflake_account_role.system_admin_role.name
  on_account_object {
    object_type = "INTEGRATION"
    object_name = local.catalog_integration_name
  }

  depends_on = [ 
    snowflake_account_role.system_admin_role,
    snowflake_grant_account_role.user_system_admin,
    snowflake_execute.catalog_integration
  ]
}
