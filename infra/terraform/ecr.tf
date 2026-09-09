locals {
  services = [
    "user-ms",
    "product-ms",
    "cart-ms",
    "order-ms",
    "payment-ms",
    "shipment-ms",
    "notification-ms",
    "frontend",
  ]
}

# One registry per image, immutable tags, scan on push: a given SHA can never
# be overwritten with different bytes, so the image that was tested is the
# image that is running (Koda docker.md).

resource "aws_ecr_repository" "ecom" {
  for_each             = toset(local.services)
  name                 = "ecom/${each.value}"
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }
}

resource "aws_ecr_lifecycle_policy" "ecom" {
  for_each   = aws_ecr_repository.ecom
  repository = each.value.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep the last 30 images per service"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 30
      }
      action = { type = "expire" }
    }]
  })
}

data "aws_caller_identity" "current" {}

locals {
  ecr_base = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${var.region}.amazonaws.com"
}