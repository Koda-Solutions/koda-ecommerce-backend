output "host_public_ip" {
  description = "The elastic IP backing the DNS record"
  value       = aws_eip.app.public_ip
}

output "host_instance_id" {
  description = "Instance id for SSM and Terraform destroy"
  value       = aws_instance.app.id
}

output "ecr_base" {
  description = "ECR registry prefix for the deploy workflows"
  value       = local.ecr_base
}

output "log_group" {
  description = "CloudWatch log group for the host"
  value       = aws_cloudwatch_log_group.ecom.name
}

output "deploy_role_arn" {
  description = "Role the GitHub workflows assume"
  value       = aws_iam_role.deploy.arn
}