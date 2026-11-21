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
  region = "us-east-1"
}

resource "aws_vpc" "prod-vpc" {
  cidr_block = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true
}

resource "aws_subnet" "prod-subnet-public-01" {
  vpc_id = aws_vpc.prod-vpc.id
  cidr_block = "10.0.1.0/24"
  availability_zone = "us-east-1a"
  map_public_ip_on_launch = true
}

resource "aws_subnet" "prod-subnet-public-02" {
  vpc_id = aws_vpc.prod-vpc.id
  cidr_block = "10.0.2.0/24"
  availability_zone = "us-east-1b"
  map_public_ip_on_launch = true
}

resource "aws_subnet" "prod-subnet-private-01" {
  vpc_id = aws_vpc.prod-vpc.id
  cidr_block = "10.0.11.0/24"
  availability_zone = "us-east-1a"
  map_public_ip_on_launch = false
}

resource "aws_subnet" "prod-subnet-private-02" {
  vpc_id = aws_vpc.prod-vpc.id
  cidr_block = "10.0.4.0/24"
  availability_zone = "us-east-1b"
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
  db_name = "prodmysqldb"
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

# eks creation 

# but first we need to create the iam role for EKS Cluster (EKS Cluster IAM Role : AmazonEKSClusterPolicy) and EKS Node (EKS Node Group IAM Role: AmazonEKSWorkerNodePolicy, AmazonEC2ContainerRegistryPullOnly, AmazonEKS_CNI_Policy)

resource "aws_iam_role" "prod-eks-cluster-role" {
  name = "prod-eks-cluster-role"
  assume_role_policy = jsonencode({
    "Version" = "2012-10-17"
    "Statement" =[{
        Effect = "Allow"
        Principal = {
            Service = "eks.amazonaws.com"
        }
        Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment"  "prod-eks-cluster-role-policy-attachment" {
    role = aws_iam_role.prod-eks-cluster-role.name
    policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
}

resource "aws_iam_role" "prod-eks-node-role" {
  name = "prod-eks-node-role"
  assume_role_policy = jsonencode({
    "Version" = "2012-10-17"
    "Statement" = [{
        Effect = "Allow"
        Principal = {
            Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "prod-eks-node-role-policy-attachment" {
  role = aws_iam_role.prod-eks-node-role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
}

resource "aws_iam_role_policy_attachment" "prod-eks-node-ecr-policy-attachment" {
  role = aws_iam_role.prod-eks-node-role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryPullOnly"
}

resource "aws_iam_role_policy_attachment" "prod-eks-node-cni-policy-attachment" {
  role = aws_iam_role.prod-eks-node-role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
}

# now we have to create the eks master and worker nodes

resource "aws_eks_cluster" "prod_eks" {
  name = "prod-eks-paywave-cluster"
  role_arn = aws_iam_role.prod-eks-cluster-role.arn
  vpc_config {
    subnet_ids = [
        aws_subnet.prod-subnet-private-01.id,
        aws_subnet.prod-subnet-private-02.id
    ]
    endpoint_private_access = true
    endpoint_public_access = true
    public_access_cidrs = ["65.0.138.49/32"]
  }
  depends_on = [
    aws_iam_role_policy_attachment.prod-eks-cluster-role-policy-attachment
  ]
}

resource "aws_eks_node_group" "prod-eks-worker-node" {
  cluster_name = aws_eks_cluster.prod_eks.name
  node_group_name = "prod-eks-worker-node"
  node_role_arn = aws_iam_role.prod-eks-node-role.arn
  instance_types = ["t3.small"]
  subnet_ids = [
    aws_subnet.prod-subnet-private-01.id,
    aws_subnet.prod-subnet-private-02.id
  ]
  scaling_config {
    desired_size = 2
    max_size = 2
    min_size = 1
  }
  depends_on = [
    aws_iam_role_policy_attachment.prod-eks-node-role-policy-attachment, aws_iam_role_policy_attachment.prod-eks-node-ecr-policy-attachment, aws_iam_role_policy_attachment.prod-eks-node-cni-policy-attachment 
  ]
}



