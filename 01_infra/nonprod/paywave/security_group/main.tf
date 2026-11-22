provider "aws" {
  region = "us-east-1"
}

terraform {
  backend "s3" {
    bucket = "infra-locking-bucket-18-11-03"
    key = "nonprod/paywave/security_group/security_group.tfstate"
    use_lockfile = true
    region = "ap-south-1"
  }
}

data "terraform_remote_state" "vpc_data"{
    backend = "s3"
    
   config = {
      region = "ap-south-1"
      bucket = "infra-locking-bucket-18-11-03"
      key = "nonprod/paywave/network/vpc.tfstate"
   }

}

module "security_group" {
  source = "https://github.com/suryakant1811/Terraform-master-repo.git//aws-security-group"

  name = "paywave-security-group"
  description = "Security group for paywave"
  vpc_id = data.terraform_remote_state.vpc_data.outputs.vpc_id

  ingress_rules = [
      {
        from_port = 22
        to_port = 22
        cidr_blocks = ["0.0.0.0/0"]
        protocol = "tcp"
        description = "Allow SSH traffic from the internet"
      },
      {
        from_port = 80
        to_port = 80
        cidr_blocks = ["0.0.0.0/0"]
        protocol = "tcp"
        description = "Allow HTTP traffic from the internet"
      },
      {
        from_port = 443
        to_port = 443
        cidr_blocks = ["0.0.0.0/0"]
        protocol = "tcp"
        description = "Allow HTTPS traffic from the internet"
      }    
  ]
  allow_all_egress = true
}