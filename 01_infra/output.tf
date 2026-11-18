
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

