# =====================================================================
# Application tier: one EC2 per microservice. Each VM clones the
# repository, builds the service's Docker image and runs it.
# =====================================================================

locals {
  clone_repo = <<-EOT
    sudo mkdir -p /labs && cd /labs
    if [ ! -d OptiCloud-Sprint4 ]; then
      git clone --branch ${var.repository_branch} ${var.repository_url}
    fi
  EOT
}

# -----------------------------------------------------------------
# notification-service (FastAPI)
# -----------------------------------------------------------------
resource "aws_instance" "notification_service" {
  ami                         = local.ami_id
  instance_type               = var.instance_type
  associate_public_ip_address = true
  key_name                    = var.key_name != "" ? var.key_name : null
  vpc_security_group_ids      = [aws_security_group.apps_http.id, aws_security_group.ssh.id]

  user_data = <<-EOT
    #!/bin/bash
    ${local.install_docker}
    ${local.clone_repo}
    cd /labs/OptiCloud-Sprint4/services/notification-service
    docker build -t opticloud/notification-service .
    docker run -d --restart=always --name notification-service \
      -p 8080:8080 \
      -e LOG_SMTP_ONLY=true \
      -e SECURITY_RECIPIENTS=security@opticloud.local \
      opticloud/notification-service
  EOT

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-notification-service"
    Role = "notification-service"
  })
}

# -----------------------------------------------------------------
# auth-service (NestJS)
# -----------------------------------------------------------------
resource "aws_instance" "auth_service" {
  ami                         = local.ami_id
  instance_type               = var.instance_type
  associate_public_ip_address = true
  key_name                    = var.key_name != "" ? var.key_name : null
  vpc_security_group_ids      = [aws_security_group.apps_http.id, aws_security_group.ssh.id]

  user_data = <<-EOT
    #!/bin/bash
    ${local.install_docker}
    ${local.clone_repo}
    cd /labs/OptiCloud-Sprint4/services/auth-service
    docker build -t opticloud/auth-service .
    docker run -d --restart=always --name auth-service \
      -p 3000:3000 \
      -e PORT=3000 \
      -e MONGO_URI=mongodb://${aws_instance.datastores.private_ip}:27017/opticloud_auth \
      -e JWT_SECRET=${var.jwt_secret} \
      -e JWT_TTL_SECONDS=3600 \
      -e NOTIFICATION_URL=http://${aws_instance.notification_service.private_ip}:8080/notifications \
      opticloud/auth-service
  EOT

  depends_on = [aws_instance.datastores, aws_instance.notification_service]

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-auth-service"
    Role = "auth-service"
  })
}

# -----------------------------------------------------------------
# reports-service (Spring Boot + Postgres + Redis)
# -----------------------------------------------------------------
resource "aws_instance" "reports_service" {
  ami                         = local.ami_id
  instance_type               = var.instance_type
  associate_public_ip_address = true
  key_name                    = var.key_name != "" ? var.key_name : null
  vpc_security_group_ids      = [aws_security_group.apps_http.id, aws_security_group.ssh.id]

  user_data = <<-EOT
    #!/bin/bash
    ${local.install_docker}
    ${local.clone_repo}
    cd /labs/OptiCloud-Sprint4/services/reports-service
    docker build -t opticloud/reports-service .
    docker run -d --restart=always --name reports-service \
      -p 8080:8080 \
      -e SERVER_PORT=8080 \
      -e REPORTS_DB_HOST=${aws_db_instance.postgres_reports.address} \
      -e REPORTS_DB_PORT=5432 \
      -e REPORTS_DB_NAME=reports_db \
      -e REPORTS_DB_USER=reports_user \
      -e REPORTS_DB_PASSWORD=${var.postgres_password} \
      -e REDIS_HOST=${aws_instance.datastores.private_ip} \
      -e REDIS_PORT=6379 \
      -e REPORTS_CACHE_TTL_MS=60000 \
      opticloud/reports-service
  EOT

  depends_on = [aws_db_instance.postgres_reports, aws_instance.datastores]

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-reports-service"
    Role = "reports-service"
  })
}

# -----------------------------------------------------------------
# normalization-service (Spring Boot)
# -----------------------------------------------------------------
resource "aws_instance" "normalization_service" {
  ami                         = local.ami_id
  instance_type               = var.instance_type
  associate_public_ip_address = true
  key_name                    = var.key_name != "" ? var.key_name : null
  vpc_security_group_ids      = [aws_security_group.apps_http.id, aws_security_group.ssh.id]

  user_data = <<-EOT
    #!/bin/bash
    ${local.install_docker}
    ${local.clone_repo}
    cd /labs/OptiCloud-Sprint4/services/normalization-service
    docker build -t opticloud/normalization-service .
    docker run -d --restart=always --name normalization-service \
      -p 8080:8080 \
      -e SERVER_PORT=8080 \
      -e REPORTS_SERVICE_URL=http://${aws_instance.reports_service.private_ip}:8080/reports \
      -e REPORTS_TIMEOUT_MS=5000 \
      opticloud/normalization-service
  EOT

  depends_on = [aws_instance.reports_service]

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-normalization-service"
    Role = "normalization-service"
  })
}

# -----------------------------------------------------------------
# cloud-adapter (Django + Postgres)
# -----------------------------------------------------------------
resource "aws_instance" "cloud_adapter" {
  ami                         = local.ami_id
  instance_type               = var.instance_type
  associate_public_ip_address = true
  key_name                    = var.key_name != "" ? var.key_name : null
  vpc_security_group_ids      = [aws_security_group.apps_http.id, aws_security_group.ssh.id]

  user_data = <<-EOT
    #!/bin/bash
    ${local.install_docker}
    ${local.clone_repo}
    cd /labs/OptiCloud-Sprint4/services/cloud-adapter
    docker build -t opticloud/cloud-adapter .
    docker run -d --restart=always --name cloud-adapter \
      -p 8080:8080 \
      -e ADAPTER_DB_HOST=${aws_db_instance.postgres_adapter.address} \
      -e ADAPTER_DB_PORT=5432 \
      -e ADAPTER_DB_NAME=adapter_db \
      -e ADAPTER_DB_USER=adapter_user \
      -e ADAPTER_DB_PASSWORD=${var.postgres_password} \
      -e NORMALIZATION_SERVICE_URL=http://${aws_instance.normalization_service.private_ip}:8080/normalize \
      opticloud/cloud-adapter
  EOT

  depends_on = [aws_db_instance.postgres_adapter, aws_instance.normalization_service]

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-cloud-adapter"
    Role = "cloud-adapter"
  })
}

# -----------------------------------------------------------------
# data-injestion (Django)
# -----------------------------------------------------------------
resource "aws_instance" "data_injestion" {
  ami                         = local.ami_id
  instance_type               = var.instance_type
  associate_public_ip_address = true
  key_name                    = var.key_name != "" ? var.key_name : null
  vpc_security_group_ids      = [aws_security_group.apps_http.id, aws_security_group.ssh.id]

  user_data = <<-EOT
    #!/bin/bash
    ${local.install_docker}
    ${local.clone_repo}
    cd /labs/OptiCloud-Sprint4/services/data-injestion
    docker build -t opticloud/data-injestion .
    docker run -d --restart=always --name data-injestion \
      -p 8080:8080 \
      -e REPORTS_SERVICE_URL=http://${aws_instance.reports_service.private_ip}:8080/reports \
      -e AUTH_SERVICE_URL=http://${aws_instance.auth_service.private_ip}:3000/auth \
      opticloud/data-injestion
  EOT

  depends_on = [aws_instance.reports_service, aws_instance.auth_service]

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-data-injestion"
    Role = "data-injestion"
  })
}
