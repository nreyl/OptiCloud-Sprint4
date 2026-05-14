variable "region" {
  description = "AWS region where the Sprint 4 infrastructure is deployed."
  type        = string
  default     = "us-east-1"
}

variable "project_prefix" {
  description = "Prefix used to name every AWS resource."
  type        = string
  default     = "opticloud"
}

variable "instance_type" {
  description = "EC2 instance type for application hosts."
  type        = string
  default     = "t3.small"
}

variable "db_instance_type" {
  description = "EC2 instance type for database hosts."
  type        = string
  default     = "t3.small"
}

variable "key_name" {
  description = "EC2 key pair name (set empty string to skip)."
  type        = string
  default     = ""
}

variable "repository_url" {
  description = "Git URL of the OptiCloud Sprint 4 repository the VMs will clone."
  type        = string
  default     = "https://github.com/nreyl/OptiCloud-Sprint4.git"
}

variable "repository_branch" {
  description = "Branch to check out on each VM."
  type        = string
  default     = "main"
}

variable "postgres_password" {
  description = "Password used for the PostgreSQL databases provisioned for reports and cloud-adapter."
  type        = string
  default     = "isis2503"
  sensitive   = true
}

variable "jwt_secret" {
  description = "Secret used by auth-service to sign JWTs."
  type        = string
  default     = "change-me-in-prod"
  sensitive   = true
}
