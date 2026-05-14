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

# t2.small = 1 vCPU / 2 GB RAM. The whole stack is 11 instances; at 1 vCPU
# each that is 11 vCPU, which fits the default 16-vCPU "Standard instances"
# quota of a fresh AWS account. Every t3.* size is 2 vCPU, so t3 would need
# 22 vCPU and a quota increase.
variable "instance_type" {
  description = "EC2 instance type for application hosts (free-tier eligible t2.micro recommended for AWS Academy lab environments)."
  type        = string
  default     = "t2.micro"
}

variable "db_instance_type" {
  description = "EC2 instance type for database hosts (free-tier eligible t2.micro recommended for AWS Academy lab environments)."
  type        = string
  default     = "t2.micro"
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
