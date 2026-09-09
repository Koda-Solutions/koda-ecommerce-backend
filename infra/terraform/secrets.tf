# Secrets live in SSM Parameter Store, never in Terraform state or in git.
# The host reads them at boot; changing a secret does not mean a rebuild
# (Koda infra).

resource "random_password" "jwt" {
  length  = 48
  special = false
}

resource "aws_ssm_parameter" "jwt_secret" {
  name  = "/ecom/dev/jwt_secret"
  type  = "SecureString"
  value = random_password.jwt.result
  tags  = { Name = "ecom-jwt-secret" }
}

resource "aws_ssm_parameter" "mysql_root_password" {
  name  = "/ecom/dev/mysql_root_password"
  type  = "SecureString"
  value = "koda_root_pw"
  tags  = { Name = "ecom-mysql-root-password" }
}