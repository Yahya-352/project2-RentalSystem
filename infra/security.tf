resource "aws_security_group" "ec2" {
  name        = "${var.project_name}-ec2-sg"
  description = "App server: public API ports"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "Service APIs"
    from_port   = 8081
    to_port     = 8086
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = { Name = "${var.project_name}-ec2-sg" }
}

resource "aws_security_group" "rds" {
  name        = "${var.project_name}-rds-sg"
  description = "Database: only reachable from the app server"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL from EC2 only"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ec2.id]
  }

  tags = { Name = "${var.project_name}-rds-sg" }
}