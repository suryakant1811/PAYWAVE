output "repository_url_map" {
  description = "Repository name -> URL (docker push/pull)"
  value       = module.paywave_ecr_repos.repository_url_map
}

output "repository_id_map" {
  description = "Repository name -> ARN"
  value       = module.paywave_ecr_repos.repository_arn_map
}