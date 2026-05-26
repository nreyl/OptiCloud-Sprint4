# =====================================================================
# Kong API Gateway (DB-less). Reads kong.yaml from the repository and
# substitutes the upstream placeholders with the private IPs that the
# rest of the stack just received.
# =====================================================================

# reports-service is scaled to var.reports_replicas instances. Render one
# Kong upstream target per replica so Kong round-robins reads across them
# (ASR1). The <REPORTS_TARGETS> sentinel line in kong.yaml is replaced with
# this block; \n produces real newlines under GNU sed on the Ubuntu AMI.
locals {
  reports_targets_yaml = join("\\n", [
    for ip in aws_instance.reports_service[*].private_ip :
    "      - target: ${ip}:8080\\n        weight: 100"
  ])
}

resource "aws_instance" "kong" {
  ami                         = local.ami_id
  instance_type               = var.instance_type
  associate_public_ip_address = true
  key_name                    = var.key_name != "" ? var.key_name : null
  vpc_security_group_ids      = [aws_security_group.kong.id, aws_security_group.ssh.id]

  user_data = <<-EOT
    #!/bin/bash
    ${local.install_docker}
    ${local.clone_repo}
    cd /labs/OptiCloud-Sprint4/kong

    sudo sed -i "s/<AUTH_HOST>/${aws_instance.auth_service.private_ip}/g" kong.yaml
    sudo sed -i "s/<INGESTION_HOST>/${aws_instance.data_injestion.private_ip}/g" kong.yaml
    sudo sed -i "s|<REPORTS_TARGETS>|${local.reports_targets_yaml}|" kong.yaml
    sudo sed -i "s/<ADAPTER_AWS_HOST>/${aws_instance.adapter_aws.private_ip}/g" kong.yaml
    sudo sed -i "s|<JWT_SECRET>|${var.jwt_secret}|g" kong.yaml

    docker network create kong-net || true
    docker run -d --restart=always --name kong --network=kong-net \
      -v "$(pwd):/kong/declarative/" \
      -e "KONG_DATABASE=off" \
      -e "KONG_DECLARATIVE_CONFIG=/kong/declarative/kong.yaml" \
      -p 8000:8000 \
      kong/kong-gateway:3.7
  EOT

  depends_on = [
    aws_instance.auth_service,
    aws_instance.data_injestion,
    aws_instance.reports_service,
    aws_instance.adapter_aws,
  ]

  tags = merge(local.common_tags, {
    Name = "${var.project_prefix}-kong"
    Role = "api-gateway"
  })
}
