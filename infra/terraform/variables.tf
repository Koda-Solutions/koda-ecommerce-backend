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

variable "github_org_id" {
  description = "Numeric GitHub organisation id, embedded by GitHub in the immutable OIDC subject claim"
  type        = string
  default     = "255325435"
}

variable "github_repos" {
  description = "Repos allowed to assume the deploy role, mapped to their numeric GitHub ids (also part of the immutable subject claim)"
  type        = map(string)
  default = {
    "koda-ecommerce-backend"  = "1363198570"
    "koda-ecommerce-frontend" = "1363198703"
  }
}