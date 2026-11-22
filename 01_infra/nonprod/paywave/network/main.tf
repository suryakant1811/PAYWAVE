provider "aws" {
  region = "us-east-1"
}

terraform {
  backend "s3" {
      bucket = "infra-locking-bucket-18-11-03"
      key = "nonprod/paywave/network/vpc.tfstate"
      region = "ap-south-1"
      use_lockfile = true
  }
}

module "vpc" {
    source = "https://github.com/suryakant1811/Terraform-master-repo.git//aws-vpc"
    vpc_cidr = "10.0.0.0/16"
    enable_dns_support = true
    enable_dns_hostnames = true

    azs = ["us-east-1a", "us-east-1b"]
    public_subnets = ["10.0.1.0/24", "10.0.2.0/24"]
    private_subnets = ["10.0.11.0/24", "10.0.4.0/24"]
    database_subnets = ["10.0.21.0/24", "10.0.22.0/24"]

}
