# =====================================================================
# Stateful tier: PostgreSQL (reports & adapter), MongoDB (auth), Redis
# =====================================================================
# Each datastore is provisioned as an EC2 instance running the official
# image in Docker. The deployment diagram shows RDS for the PostgreSQL
# instances; for AWS Academy LabRole environments RDS is restricted, so
# we use EC2+Docker uniformly which is functionally equivalent for the
# coursework while remaining trivial to swap for `aws_db_instance` later.

resource "aws_instance" "postgres_reports" {
  ami                         = local.ami_id
  instance_type               = var.db_instance_type
  associate_public_ip_address = true
  key_name                    = var.key_name != "" ? var.key_name : null
  vpc_security_group_ids      = [aws_security_group.postgres.id, aws_security_group.ssh.id]

  user_data = <<-EOT
    #!/bin/bash
    ${local.install_docker}
    docker run -d --restart=always --name postgres-reports \
      -e POSTGRES_USER=reports_user \
      -e POSTGRES_DB=reports_db \
      -e POSTGRES_PASSWORD=${var.postgres_password} \
      -p 5432:5432 \
      postgres:16
  EOT

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-postgres-reports"
    Role = "postgres-reports"
  })
}

resource "aws_instance" "postgres_adapter" {
  ami                         = local.ami_id
  instance_type               = var.db_instance_type
  associate_public_ip_address = true
  key_name                    = var.key_name != "" ? var.key_name : null
  vpc_security_group_ids      = [aws_security_group.postgres.id, aws_security_group.ssh.id]

  user_data = <<-EOT
    #!/bin/bash
    ${local.install_docker}
    docker run -d --restart=always --name postgres-adapter \
      -e POSTGRES_USER=adapter_user \
      -e POSTGRES_DB=adapter_db \
      -e POSTGRES_PASSWORD=${var.postgres_password} \
      -p 5432:5432 \
      postgres:16
  EOT

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-postgres-adapter"
    Role = "postgres-adapter"
  })
}

resource "aws_instance" "mongo_auth" {
  ami                         = local.ami_id
  instance_type               = var.db_instance_type
  associate_public_ip_address = true
  key_name                    = var.key_name != "" ? var.key_name : null
  vpc_security_group_ids      = [aws_security_group.mongo.id, aws_security_group.ssh.id]

  user_data = <<-EOT
    #!/bin/bash
    ${local.install_docker}
    docker run -d --restart=always --name mongo-auth \
      -p 27017:27017 \
      mongo:7
  EOT

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-mongo-auth"
    Role = "mongo-auth"
  })
}

resource "aws_instance" "redis" {
  ami                         = local.ami_id
  instance_type               = var.db_instance_type
  associate_public_ip_address = true
  key_name                    = var.key_name != "" ? var.key_name : null
  vpc_security_group_ids      = [aws_security_group.redis.id, aws_security_group.ssh.id]

  user_data = <<-EOT
    #!/bin/bash
    ${local.install_docker}
    docker run -d --restart=always --name redis-reports \
      -p 6379:6379 \
      redis:7-alpine
  EOT

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-redis"
    Role = "redis"
  })
}
