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
  description = "EC2 instance type for application hosts (free-tier eligible t3.micro recommended for AWS Academy lab environments)."
  type        = string
  default     = "t3.micro"
}

variable "db_instance_type" {
  description = "EC2 instance type for database hosts (free-tier eligible t3.micro recommended for AWS Academy lab environments)."
  type        = string
  default     = "t3.micro"
}

# Cold-path services (notification, normalization, adapter, ingestion) are idle
# during the ASR1 latency test, so they run on 1-vCPU t2.micro. That frees
# vCPU under the 16-vCPU account quota for the extra reports-service replicas
# below. Budget: 4 cold (1 each) + auth/kong/datastores (2 each) + reports*3
# (2 each) = 16 vCPU exactly.
variable "cold_instance_type" {
  description = "EC2 instance type for services not on the ASR1 hot path (1 vCPU is enough; keeps the stack within the 16-vCPU quota)."
  type        = string
  default     = "t2.micro"
}

# reports-service is horizontally scaled behind Kong's reports_upstream
# (round-robin). 3 replicas is the ASR1 target topology. Raising this also
# means adding a matching target placeholder is unnecessary — kong.tf renders
# the upstream targets dynamically from this count.
variable "reports_replicas" {
  description = "Number of reports-service instances load-balanced by Kong for ASR1 (read throughput)."
  type        = number
  default     = 3
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

# bcrypt work factor. login does one bcrypt.compare per request and the ASR1
# test logs in 5000 users during ramp-up; at the prod default of 10 (~80ms of
# pure CPU each) that storm saturates the auth-service vCPU and the resulting
# 401s break the measured /spend sampler. 6 keeps brute-force cost reasonable
# while letting the login ramp keep up. Raise back to 10+ for a real deployment.
variable "bcrypt_rounds" {
  description = "bcrypt cost factor for auth-service password hashing (lowered for the ASR1 load test; use 10+ in production)."
  type        = number
  default     = 6
}
