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
output "cloud_adapter_public_ip"        { value = aws_instance.cloud_adapter.public_ip }
output "normalization_service_public_ip"{ value = aws_instance.normalization_service.public_ip }
output "reports_service_public_ip"      { value = aws_instance.reports_service.public_ip }

output "postgres_reports_private_ip" { value = aws_instance.postgres_reports.private_ip }
output "postgres_adapter_private_ip" { value = aws_instance.postgres_adapter.private_ip }
output "mongo_auth_private_ip"       { value = aws_instance.mongo_auth.private_ip }
output "redis_private_ip"            { value = aws_instance.redis.private_ip }
