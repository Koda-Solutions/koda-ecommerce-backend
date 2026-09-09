# GitHub Actions deploys through OIDC. There is no stored registry or AWS
# credential anywhere (Koda docker.md): the workflow assumes a short-lived
# role scoped to this project's two repos.

# The OIDC provider already exists in this account, created for Koda's own
# projects. One provider per issuer per account: reference it, never manage it
# (Koda cicd.tf). Managing a thumbprint list here would drift and could break
# Koda's own deploys.
data "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"
}

resource "aws_iam_role" "deploy" {
  name = "ecom-deploy-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = data.aws_iam_openid_connect_provider.github.arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = { "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com" }
        StringLike = {
          # GitHub mints the "immutable" subject form carrying both ids, not
          # the documented repo:ORG/REPO form. Verified against a real token
          # (Koda cicd.tf): repo:Koda-Solutions@255325435/koda-ecommerce-backend@1363198570:ref:refs/heads/main
          "token.actions.githubusercontent.com:sub" = concat(
            [
              for repo, repo_id in var.github_repos :
              "repo:${var.github_org}@${var.github_org_id}/${repo}@${repo_id}:ref:refs/heads/main"
            ],
          )
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "deploy" {
  name = "ecom-push-and-deploy"
  role = aws_iam_role.deploy.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:GetRepositoryPolicy",
          "ecr:DescribeRepositories",
          "ecr:ListImages",
          "ecr:DescribeImages",
          "ecr:BatchGetImage",
          "ecr:InitiateLayerUpload",
          "ecr:UploadLayerPart",
          "ecr:CompleteLayerUpload",
          "ecr:PutImage",
          "ecr:GetLifecyclePolicyPreview",
          "ecr:ListTagsForResource",
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ssm:SendCommand",
          "ssm:DescribeInstanceInformation",
          "ssm:GetCommandInvocation",
          "ec2:DescribeInstances",
          "ec2:DescribeTags",
        ]
        Resource = "*"
      },
    ]
  })
}