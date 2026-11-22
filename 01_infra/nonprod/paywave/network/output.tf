output "vpc_id" {
  description = "ID of the VPC."
  value       = module.vpc.vpc_id
}

output "vpc_cidr" {
  description = "CIDR block of the VPC."
  value       = module.vpc.vpc_cidr
}

output "public_subnet_ids" {
  description = "Public subnet IDs keyed as public-01, public-02, and so on."
  value       = { for key, subnet in module.vpc.public_subnet_ids : key => subnet.id }
}

output "private_subnet_ids" {
  description = "Private subnet IDs keyed as private-01, private-02, and so on."
  value       = { for key, subnet in module.vpc.private_subnet_ids : key => subnet.id }
}

output "private_subnet_cidrs" {
  description = "Private subnet CIDRs keyed as private-01, private-02, and so on."
  value       = { for key, subnet in module.vpc.private_subnet_cidrs : key => subnet.cidr_block }
}

output "database_subnet_ids" {
  description = "Database subnet IDs keyed as database-01, database-02, and so on."
  value       = { for key, subnet in module.vpc.database_subnet_ids : key => subnet.id }
}

output "database_subnet_cidrs" {
  description = "Database subnet CIDRs keyed as database-01, database-02, and so on."
  value       = { for key, subnet in module.vpc.database_subnet_cidrs : key => subnet.cidr_block }
}

output "nat_gateway_id" {
  description = "NAT gateway ID, or null when disabled."
  value       = try(module.vpc.nat_gateway_id, null)
}

output "public_route_table_id" {
  description = "ID of the public route table."
  value       = module.vpc.public_route_table_id
}

output "private_route_table_id" {
  description = "ID of the private route table."
  value       = module.vpc.private_route_table_id
}

output "database_route_table_id" {
  description = "ID of the database route table."
  value       = module.vpc.database_route_table_id
}