resource "random_password" "jwt" {
  length  = 64
  special = false
}

resource "random_password" "internal_api_key" {
  length  = 32
  special = false
}

locals {
  app_secrets = {
    DB_USERNAME            = var.db_username
    JWT_SECRET             = random_password.jwt.result
    INTERNAL_API_KEY       = random_password.internal_api_key.result
    SMTP_USERNAME          = var.smtp_username
    SMTP_PASSWORD          = var.smtp_password
    ADMIN_USERNAME         = var.admin_username
    ADMIN_PASSWORD         = var.admin_password
    SEED_PASSWORD          = var.seed_password
    STRIPE_TEST_SECRET_KEY = var.stripe_secret_key
    STRIPE_WEB_HOOK_SECRET = var.stripe_webhook_secret
    FRONTEND_URL           = var.frontend_url != "" ? var.frontend_url : "https://${aws_cloudfront_distribution.main.domain_name}"
  }
}

resource "aws_ssm_parameter" "app" {
  for_each = nonsensitive(toset(keys(local.app_secrets)))

  name  = "/${var.project_name}/app/${each.key}"
  type  = "SecureString"
  value = local.app_secrets[each.key]
}