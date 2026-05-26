# =====================================================================
# Stateful tier
#
# - The two PostgreSQL databases are provisioned as managed RDS instances
#   (db.t3.micro, free-tier-eligible). They do not count against the EC2
#   vCPU quota.
# - MongoDB and Redis run as Docker containers on a single shared EC2
#   ("datastores"); RDS does not offer those engines.
#
# Total: 2 RDS + 1 EC2 (instead of 4 EC2 in the previous topology).
# =====================================================================

# Default VPC + its subnets (used to build the RDS subnet group). The
# default VPC has one subnet per Availability Zone which is what RDS
# expects.
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

resource "aws_db_subnet_group" "default" {
  name       = "${var.project_prefix}-db-subnets"
  subnet_ids = data.aws_subnets.default.ids

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-db-subnets"
  })
}

# ---------------------------------------------------------------------
# PostgreSQL for reports-service (managed)
# ---------------------------------------------------------------------
resource "aws_db_instance" "postgres_reports" {
  identifier              = "${var.project_prefix}-postgres-reports"
  engine                  = "postgres"
  instance_class          = "db.t3.micro"
  allocated_storage       = 20
  storage_type            = "gp2"
  db_name                 = "reports_db"
  username                = "reports_user"
  password                = var.postgres_password
  db_subnet_group_name    = aws_db_subnet_group.default.name
  vpc_security_group_ids  = [aws_security_group.postgres.id]
  publicly_accessible     = true
  skip_final_snapshot     = true
  backup_retention_period = 0
  apply_immediately       = true

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-postgres-reports"
    Role = "postgres-reports"
  })
}

# ---------------------------------------------------------------------
# PostgreSQL for cloud-adapter (managed)
# ---------------------------------------------------------------------
resource "aws_db_instance" "postgres_adapter" {
  identifier              = "${var.project_prefix}-postgres-adapter"
  engine                  = "postgres"
  instance_class          = "db.t3.micro"
  allocated_storage       = 20
  storage_type            = "gp2"
  db_name                 = "adapter_db"
  username                = "adapter_user"
  password                = var.postgres_password
  db_subnet_group_name    = aws_db_subnet_group.default.name
  vpc_security_group_ids  = [aws_security_group.postgres.id]
  publicly_accessible     = true
  skip_final_snapshot     = true
  backup_retention_period = 0
  apply_immediately       = true

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-postgres-adapter"
    Role = "postgres-adapter"
  })
}

# ---------------------------------------------------------------------
# Mongo + Redis colocated on a single EC2 (Docker)
# ---------------------------------------------------------------------
resource "aws_instance" "datastores" {
  ami                         = local.ami_id
  instance_type               = var.db_instance_type
  associate_public_ip_address = true
  key_name                    = var.key_name != "" ? var.key_name : null
  vpc_security_group_ids = [
    aws_security_group.mongo.id,
    aws_security_group.redis.id,
    aws_security_group.ssh.id,
  ]

  user_data = <<-EOT
    #!/bin/bash
    ${local.install_docker}
    docker run -d --restart=always --name mongo-auth \
      -p 27017:27017 \
      mongo:7
    docker run -d --restart=always --name redis-reports \
      -p 6379:6379 \
      redis:7-alpine
  EOT

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-datastores"
    Role = "mongo+redis"
  })
}
