output "ec2_public_ip" {
  value = aws_instance.app.public_ip
}

output "rds_endpoint" {
  value = aws_db_instance.main.address
}

output "ecr_registry" {
  value = split("/", aws_ecr_repository.service["user-service"].repository_url)[0]
}

output "frontend_url" {
  value = "http://${aws_s3_bucket_website_configuration.frontend.website_endpoint}"
}

output "app_url" {
  value = "https://${aws_cloudfront_distribution.main.domain_name}"
}

output "log_group" {
  value = aws_cloudwatch_log_group.app.name
}