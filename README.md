# koda-ecommerce-backend

**Live at https://ecommerce.kodasolutions.net** - seven Spring Boot microservices behind one Caddy gateway, deployed on every push to main.

The backend for the Koda ecommerce storefront: separate deployable services
for users, catalog, cart, orders, payments, shipments and notifications, each
with its own schema and least-privilege MySQL account. One schema per service,
one port per service, one pipeline that takes a git push to a running
production stack.

## Repository layout

```
services/       seven Maven modules, one per microservice
services/*/Dockerfile   multi-stage, non-root, healthchecked image per service
deploy/         the production compose, Caddyfile and deploy.sh the host runs
infra/terraform the complete AWS environment (network, ECR, EC2, OIDC role)
scripts/        local MySQL for development
.github/workflows/ci.yml  verify, build, push to ECR, deploy to EC2
```

## The seven services

| Service        | Port  | Database schema   | API path          |
| -------------- | ----- | ----------------- | ----------------- |
| user-ms        | 8040  | ecommerce_user    | /api/user         |
| product-ms     | 8050  | ecommerce_product | /api/product      |
| cart-ms        | 8060  | ecommerce_cart    | /api/cart         |
| order-ms       | 8070  | ecommerce_order   | /api/order        |
| payment-ms     | 8080  | ecommerce_payment | /api/payment      |
| shipment-ms    | 8090  | ecommerce_shipment | /api/shipment    |
| notification-ms| 8100  | ecommerce_notification | /api/notification |

Each service reads its database user and password from the environment
(MYSQL_USER, MYSQL_PASSWORD, MYSQL_HOST). Nothing secret is committed.

## Run locally

Start MySQL (port 3307 to avoid a system mysqld on 3306, seeded with the
schemas and users from scripts/mysql/init):

```bash
docker compose -f scripts/docker-compose.yml up -d
```

Run one service with Maven against it:

```bash
cd services/user-ms
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:mysql://127.0.0.1:3307/ecommerce_user"
```

Repeat for the other services with their own schema and port.

## Test

```bash
mvn -B verify
```

The reactor compiles and tests all seven modules against a MySQL service.
The same command runs in CI with MySQL provisioned by the workflow.

## Build and push images

```bash
docker build -t ecom/user-ms services/user-ms
```

CI does the same for every service and tags each image with the commit SHA.

## Deploy

Push to `main` and the ci workflow (backend/.github/workflows/ci.yml):

1. Runs `mvn -B verify` against a disposable MySQL.
2. Builds all seven images and pushes SHA-tagged images to ECR
   (ecom/user-ms ... ecom/notification-ms).
3. Assumes an AWS deploy role through GitHub OIDC, so no AWS or registry
   credentials ever live in the repository.
4. Tells the EC2 host via AWS Systems Manager to run deploy/deploy.sh, which
   swaps the running containers to the new tags behind Caddy.

Caddy answers on 80/443, terminates TLS with certificates it manages itself,
routes /api/* to the owning service container, serves the frontend SPA, and
blocks /api/*/actuator/* and /api/*/internal/* from the public internet.

## AWS environment

The whole production environment is Terraform under infra/terraform: VPC,
security groups, eight ECR repos, the EC2 instance with an Elastic IP, the
IAM deploy role and instance profile, SSM parameters and the budget alarm.
Apply it with `terraform apply` from infra/terraform.

## Frontend

See https://github.com/Koda-Solutions/koda-ecommerce-frontend for the Vue storefront.

## Status

Skeleton live: storefront behind Caddy, all seven services healthy and
deployable by push. Feature work (registration, catalog, cart, checkout,
orders, payments, shipments, notifications) continues on top of this skeleton
service by service.