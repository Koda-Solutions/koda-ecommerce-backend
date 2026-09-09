terraform {
  required_version = ">= 1.5"

  backend "s3" {
    bucket         = "ecom-tfstate-740089361110-eu-central-1"
    key            = "ecommerce/terraform.tfstate"
    region         = "eu-central-1"
    dynamodb_table = "ecom-tfstate-locks"
    encrypt        = true
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project   = "ecommerce"
      ManagedBy = "terraform"
      Owner     = var.owner_email
    }
  }
}