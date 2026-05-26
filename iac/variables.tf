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

# Free-tier-restricted accounts only allow free-tier-eligible types, and in
# this account/region that is t3.micro (2 vCPU) — t2.micro is rejected. So
# every host is t3.micro and the 16-vCPU quota caps the stack at 8 instances.
# To run 3 reports replicas (10 instances = 20 vCPU) we instead drop the
# services not on the ASR1 /spend path via var.loadtest_mode below.
variable "cold_instance_type" {
  description = "EC2 instance type for services not on the ASR1 hot path."
  type        = string
  default     = "t3.micro"
}

# reports-service is horizontally scaled behind Kong's reports_upstream
# (round-robin); kong.tf renders one upstream target per replica.
#
# vCPU budget (free-tier => every host is t3.micro = 2 vCPU; account quota 16):
#   - Default (loadtest_mode = false): full 8-service stack + 1 replica = 16 vCPU.
#   - ASR1 run  (loadtest_mode = true): notification/normalization/adapter off,
#     leaving kong+auth+datastores+ingestion (8 vCPU) + reports*N. N=3 => 14 vCPU.
# So 3 replicas REQUIRES loadtest_mode = true to stay under the 16-vCPU quota.
variable "reports_replicas" {
  description = "Number of reports-service instances load-balanced by Kong for ASR1 (read throughput). Use 3 together with loadtest_mode=true."
  type        = number
  default     = 1
}

# ASR1-only deploy: scale the services not on the /spend path (notification,
# normalization, adapter) to 0 to free vCPU for extra reports replicas under
# the 16-vCPU quota. data-injestion stays up because the JMeter seed posts the
# report through POST /ingest/reports. ASR2/ASR3 are not exercised in this mode.
variable "loadtest_mode" {
  description = "When true, disable services not needed by ASR1 to free vCPU for reports replicas (breaks ASR2/ASR3 on this deploy)."
  type        = bool
  default     = false
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
