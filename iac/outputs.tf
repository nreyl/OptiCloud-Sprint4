output "kong_public_ip" {
  description = "Public IP of the Kong API Gateway (use as base for /auth, /ingest, /reports, /adapters)."
  value       = aws_instance.kong.public_ip
}

output "kong_base_url" {
  description = "Base URL for the OptiCloud API."
  value       = "http://${aws_instance.kong.public_ip}:8000"
}

output "auth_service_public_ip"          { value = aws_instance.auth_service.public_ip }
output "notification_service_public_ip" { value = aws_instance.notification_service.public_ip }
output "data_injestion_public_ip"       { value = aws_instance.data_injestion.public_ip }
output "adapter_aws_public_ip"          { value = aws_instance.adapter_aws.public_ip }
output "normalization_service_public_ip"{ value = aws_instance.normalization_service.public_ip }
output "reports_service_public_ip"      { value = aws_instance.reports_service.public_ip }

output "postgres_reports_endpoint" {
  description = "RDS endpoint (host) for reports-service."
  value       = aws_db_instance.postgres_reports.address
}

output "postgres_adapter_endpoint" {
  description = "RDS endpoint (host) for cloud-adapter."
  value       = aws_db_instance.postgres_adapter.address
}

output "datastores_public_ip" {
  description = "Public IP of the EC2 hosting MongoDB and Redis."
  value       = aws_instance.datastores.public_ip
}

output "datastores_private_ip" {
  description = "Private IP of the EC2 hosting MongoDB and Redis (consumed by auth-service and reports-service)."
  value       = aws_instance.datastores.private_ip
}
