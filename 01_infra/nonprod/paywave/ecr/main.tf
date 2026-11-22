provider "aws" {
    region = "us-east-1"
}

terraform {
  backend "s3" {
    bucket = "infra-locking-bucket-18-11-03"
    key = "nonprod/paywave/ecr/ecr.tfstate"
    use_lockfile = true
    region = "ap-south-1"
  }
}

locals {
  images_names = [
        "paywave-prod-api-gateway",
        "paywave-prod-account-service",
        "paywave-prod-wallet-service",
        "paywave-prod-payment-service",
        "paywave-prod-settlement-service"
    ]
}

module "paywave_ecr_repos" {
    source = "https://github.com/suryakant1811/Terraform-master-repo.git//aws-ecr-repository"
    images_names = local.images_names
    scan_on_push = true
}
