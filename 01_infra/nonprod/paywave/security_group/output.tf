output "security_group_id" {
  value = module.security_group.security_group_id
}

output "mysql_security_group_id" {
  value = module.paywave_rds_security_group.security_group_id
}