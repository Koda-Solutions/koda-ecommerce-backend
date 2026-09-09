#!/usr/bin/env bash
# Creates the Terraform state bucket and DynamoDB lock table for the ecommerce
# project. Run once, ever. The bucket name is account-unique; this bucket only
# holds ecommerce state, so `terraform destroy` can never touch Koda or
# hr-portal state (Koda infra README).
set -euo pipefail

export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-eu-central-1}"
BUCKET="ecom-tfstate-740089361110-eu-central-1"
TABLE="ecom-tfstate-locks"

aws s3api create-bucket \
  --bucket "$BUCKET" \
  --region "$AWS_DEFAULT_REGION" \
  --create-bucket-configuration LocationConstraint="$AWS_DEFAULT_REGION"

aws s3api put-bucket-versioning --bucket "$BUCKET" --versioning-configuration Status=Enabled
aws s3api put-bucket-encryption --bucket "$BUCKET" \
  --server-side-encryption-configuration '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
aws s3api put-public-access-block --bucket "$BUCKET" \
  --public-access-block-configuration '{"BlockPublicAcls":true,"IgnorePublicAcls":true,"BlockPublicPolicy":true,"RestrictPublicBuckets":true}'

aws dynamodb create-table \
  --table-name "$TABLE" \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST

echo "state bucket $BUCKET and lock table $TABLE ready"