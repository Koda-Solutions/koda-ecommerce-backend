variable "region" {
  description = "eu-central-1 (Frankfurt) per the Koda latency decision: 60-80ms from Cairo vs 150-200ms from us-east-1"
  type        = string
  default     = "eu-central-1"
}

variable "vpc_cidr" {
  description = "10.30.0.0/16 so it can never collide with Koda's 10.20.0.0/16 in the same account"
  type        = string
  default     = "10.30.0.0/16"
}

variable "instance_type" {
  description = "c7i-flex.large (4 GiB): the largest free-tier eligible type in this account (the AWS Free Plan refuses anything else), sized for 7 JVMs at 280m, mysql 400m, frontend 256m, caddy 96m, still inside the kernel/container budget"
  type        = string
  default     = "c7i-flex.large"
}

variable "owner_email" {
  description = "Email for the budget alarm and Let's Encrypt (ACME) certificates"
  type        = string
  default     = "Ziad.Sharara@cic.ae"
}

variable "budget_limit" {
  description = "Monthly budget alarm at Project level, matching Koda's own 80 USD threshold"
  type        = number
  default     = 80
}

variable "github_org" {
  description = "GitHub organisation that owns the two ecommerce repos"
  type        = string
  default     = "Koda-Solutions"
}

variable "github_repos" {
  description = "Repos allowed to assume the deploy role"
  type        = list(string)
  default     = ["koda-ecommerce-backend", "koda-ecommerce-frontend"]
}