terraform {
  required_version = ">= 1.0.0"
  required_providers {
    aws = {
      source = "hashicorp/aws"
      version = ">= 4.0.0"
    }
  }
}

provider "aws" {
  region = "ap-south-1"
}

resource "aws_s3_bucket" "infra-locking-bucket" {
  bucket = "infra-locking-bucket-18-11-03"
}

resource "aws_s3_bucket_versioning" "infra-locking-bucket-versioning" {
  bucket = aws_s3_bucket.infra-locking-bucket.id
  versioning_configuration {
    status = "Enabled"
  }
}