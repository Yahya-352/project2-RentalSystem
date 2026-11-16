output "ec2_public_ip" {
  value = aws_instance.app.public_ip
}

output "rds_endpoint" {
  value = aws_db_instance.main.address
}

output "ecr_registry" {
  value = split("/", aws_ecr_repository.service["user-service"].repository_url)[0]
}