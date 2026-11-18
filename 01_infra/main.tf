terraform {
  required_version = ">= 1.0.0"
  required_providers {
    aws = {
      source = "hashicorp/aws"
      version = ">= 4.0.0"
    }
  }
  backend "s3" {
    bucket = "infra-locking-bucket-18-11-03"
    key = "terraform.tfstate"
    region = "ap-south-1"
    use_lockfile = true
  }
}

provider "aws" {
  region = "ap-south-1"
}

resource "aws_vpc" "prod-vpc" {
  cidr_block = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true
}

resource "aws_subnet" "prod-subnet-public-01" {
  vpc_id = aws_vpc.prod-vpc.id
  cidr_block = "10.0.1.0/24"
  availability_zone = "ap-south-1a"
  map_public_ip_on_launch = true
}

resource "aws_subnet" "prod-subnet-public-02" {
  vpc_id = aws_vpc.prod-vpc.id
  cidr_block = "10.0.2.0/24"
  availability_zone = "ap-south-1b"
  map_public_ip_on_launch = true
}

resource "aws_subnet" "prod-subnet-private-01" {
  vpc_id = aws_vpc.prod-vpc.id
  cidr_block = "10.0.11.0/24"
  availability_zone = "ap-south-1a"
  map_public_ip_on_launch = false
}

resource "aws_subnet" "prod-subnet-private-02" {
  vpc_id = aws_vpc.prod-vpc.id
  cidr_block = "10.0.4.0/24"
  availability_zone = "ap-south-1b"
  map_public_ip_on_launch = false
}

resource "aws_internet_gateway" "prod-igw" {
  vpc_id = aws_vpc.prod-vpc.id
}

resource "aws_route_table" "prod-rt-public" {
  vpc_id = aws_vpc.prod-vpc.id
  route{
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.prod-igw.id
  }
}

resource "aws_route_table_association" "prod-rt-association-public-01" {
  subnet_id = aws_subnet.prod-subnet-public-01.id
  route_table_id = aws_route_table.prod-rt-public.id
}

resource "aws_route_table_association" "prod-rt-association-public-02" {
  subnet_id = aws_subnet.prod-subnet-public-02.id
  route_table_id = aws_route_table.prod-rt-public.id
}

resource "aws_eip" "prod-eip" {
  domain = "vpc"
}

resource "aws_nat_gateway" "prod-ngw" {
  allocation_id = aws_eip.prod-eip.id
  subnet_id = aws_subnet.prod-subnet-public-01.id
  depends_on = [aws_internet_gateway.prod-igw]
}

resource "aws_route_table" "prod-rt-private" {
    vpc_id = aws_vpc.prod-vpc.id
    route{
        cidr_block = "0.0.0.0/0"
        nat_gateway_id = aws_nat_gateway.prod-ngw.id
    }
}

resource "aws_route_table_association" "prod-rt-association-private-01" {
    subnet_id = aws_subnet.prod-subnet-private-01.id
    route_table_id = aws_route_table.prod-rt-private.id
}

resource "aws_route_table_association" "prod-rt-association-private-02" {
    subnet_id = aws_subnet.prod-subnet-private-02.id
    route_table_id = aws_route_table.prod-rt-private.id
}

resource "aws_security_group" "prod-sg" {
  name = "prod-sg"
  description = "Security group for production"
  vpc_id = aws_vpc.prod-vpc.id
  ingress {
    from_port = 22
    to_port = 22
    protocol = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow SSH traffic from the internet"
  }
  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all traffic out of the security group"
  }
}

resource "aws_db_subnet_group" "prod-db-subnet-group" {
  name = "prod-db-subnet-group"
  description = "Subnet group for production database"
  subnet_ids = [
    aws_subnet.prod-subnet-private-01.id, 
    aws_subnet.prod-subnet-private-02.id
  ]
}

resource "aws_security_group" "prod-rds-sg" {
  name = "prod-rds-sg"
  description = "Security group for production database"
  vpc_id = aws_vpc.prod-vpc.id
  ingress {
    from_port = 3306
    to_port = 3306
    protocol = "tcp"
    cidr_blocks = [aws_subnet.prod-subnet-private-01.cidr_block, aws_subnet.prod-subnet-private-02.cidr_block]
    description = "Allow MySQL traffic from the internet"
  }
  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all traffic out of the security group"
  } 
}

resource "aws_db_instance" "prod-mysql-db" {
  identifier = "paywave-prod-mysqlc"
  engine = "mysql"
  instance_class = "db.t3.micro"
  allocated_storage = 20
  storage_type = "gp2"
  backup_retention_period = 0
  db_name = "prod-mysql-db"
  username = "admin"
  password = "admin123"
  db_subnet_group_name = aws_db_subnet_group.prod-db-subnet-group.name
  vpc_security_group_ids = [aws_security_group.prod-rds-sg.id]
  
  multi_az = false
  skip_final_snapshot = true
}

variable "ecr_repos_names" {
  type = list(string)
  default = [ 
    "prod-api-gateway",
    "prod-account-service",
    "prod-wallet-service",
    "prod-payment-service",
    "prod-settlement-service",
  ]
}

resource "aws_ecr_repository" "prod-ecr-repos" {
  for_each = toset(var.ecr_repos_names)
  name     = each.value
  image_scanning_configuration {
    scan_on_push = true
  }
  image_tag_mutability = "MUTABLE"
}

data "aws_caller_identity" "current" {}

data "aws_region" "current" {}

output "aws_account_id" {
  description = "AWS account ID used for this apply"
  value       = data.aws_caller_identity.current.account_id
}

output "aws_region" {
  description = "Region configured in the provider"
  value       = data.aws_region.current.name
}

output "vpc_id" {
  description = "Production VPC ID"
  value       = aws_vpc.prod-vpc.id
}

output "vpc_cidr_block" {
  description = "Production VPC CIDR"
  value       = aws_vpc.prod-vpc.cidr_block
}

output "public_subnet_ids" {
  description = "Public subnet IDs (ALB / NAT / bastion-style EC2)"
  value = [
    aws_subnet.prod-subnet-public-01.id,
    aws_subnet.prod-subnet-public-02.id,
  ]
}

output "private_subnet_ids" {
  description = "Private subnet IDs (RDS, EKS nodes)"
  value = [
    aws_subnet.prod-subnet-private-01.id,
    aws_subnet.prod-subnet-private-02.id,
  ]
}

output "internet_gateway_id" {
  description = "Internet gateway attached to the VPC"
  value       = aws_internet_gateway.prod-igw.id
}

output "nat_gateway_id" {
  description = "NAT gateway ID (outbound from private subnets)"
  value       = aws_nat_gateway.prod-ngw.id
}

output "nat_gateway_public_ip" {
  description = "Elastic IP used by the NAT gateway"
  value       = aws_eip.prod-eip.public_ip
}

output "prod_sg_id" {
  description = "General prod security group (e.g. ops EC2)"
  value       = aws_security_group.prod-sg.id
}

output "rds_security_group_id" {
  description = "RDS security group ID"
  value       = aws_security_group.prod-rds-sg.id
}

output "rds_identifier" {
  description = "RDS instance identifier"
  value       = aws_db_instance.prod-mysql-db.identifier
}

output "rds_endpoint" {
  description = "RDS hostname:port (use for mysql client / JDBC host)"
  value       = aws_db_instance.prod-mysql-db.endpoint
}

output "rds_address" {
  description = "RDS hostname only"
  value       = aws_db_instance.prod-mysql-db.address
}

output "rds_port" {
  description = "MySQL port"
  value       = aws_db_instance.prod-mysql-db.port
}

output "rds_db_subnet_group" {
  description = "DB subnet group name"
  value       = aws_db_subnet_group.prod-db-subnet-group.name
}

output "rds_jdbc_wallet_example" {
  description = "Example JDBC URL for wallet-service (same host, different schema per service)"
  value       = "jdbc:mysql://${aws_db_instance.prod-mysql-db.address}:${aws_db_instance.prod-mysql-db.port}/wallet_db?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=UTC"
}

output "ecr_repository_urls" {
  description = "Map of ECR repo name -> repository URL (docker push/pull)"
  value       = { for name, repo in aws_ecr_repository.prod-ecr-repos : name => repo.repository_url }
}

output "ecr_repository_arns" {
  description = "Map of ECR repo name -> ARN"
  value       = { for name, repo in aws_ecr_repository.prod-ecr-repos : name => repo.arn }
}

output "console_vpc_link" {
  description = "AWS console VPC details (open in browser when logged in)"
  value       = "https://${data.aws_region.current.id}.console.aws.amazon.com/vpc/home?region=${data.aws_region.current.id}#VpcDetails:VpcId=${aws_vpc.prod-vpc.id}"
}

output "console_rds_link" {
  description = "AWS console RDS instance"
  value       = "https://${data.aws_region.current.id}.console.aws.amazon.com/rds/home?region=${data.aws_region.current.id}#database:id=${aws_db_instance.prod-mysql-db.identifier};is-cluster=false"
}

