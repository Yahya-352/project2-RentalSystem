variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "eu-north-1"
}

variable "project_name" {
  description = "Name used in resource names and tags"
  type        = string
  default     = "rentride"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "prod"
}

variable "instance_type" {
  description = "EC2 instance type for the app server"
  type        = string
  default     = "t3.micro"
}

variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t4g.micro"
}

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "rental_db"
}

variable "db_username" {
  description = "PostgreSQL master username"
  type        = string
  default     = "rentride_admin"
}

variable "frontend_url" {
  description = "Public URL of the frontend (used for Stripe redirects); empty means the CloudFront URL"
  type        = string
  default     = ""
}

variable "smtp_username" {
  type      = string
  sensitive = true
}

variable "smtp_password" {
  type      = string
  sensitive = true
}

variable "admin_username" {
  type      = string
  sensitive = true
}

variable "admin_password" {
  type      = string
  sensitive = true
}

variable "seed_password" {
  type      = string
  sensitive = true
}

variable "stripe_secret_key" {
  type      = string
  sensitive = true
}

variable "stripe_webhook_secret" {
  type      = string
  sensitive = true
}

variable "alert_email" {
  description = "Email that receives CloudWatch alarm notifications (leave empty for none)"
  type        = string
  default     = ""
}