#!/usr/bin/env bash
# First boot of the ecommerce host, rendered by Terraform. Installs Docker and
# the ECR credential helper, writes the runtime files to /opt/ecom, and arms a
# systemd unit so the stack comes back after a reboot. Mirrors the Koda infra
# pattern: nothing on the host except Docker, Caddy as the only edge, deploy by
# pull then swap.

set -euo pipefail

exec > >(tee -a /var/log/ecom-user-data.log) 2>&1
echo "user-data started"

IMDS_TOKEN=$(curl -fsS -X PUT "http://169.254.169.254/latest/api/token" \
  -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
REGION=$(curl -fsS -H "X-aws-ec2-metadata-token: $IMDS_TOKEN" \
  http://169.254.169.254/latest/meta-data/placement/region)
ACCOUNT=$(curl -fsS -H "X-aws-ec2-metadata-token: $IMDS_TOKEN" \
  http://169.254.169.254/latest/meta-data/identity-credentials/ec2/info \
  | jq -r .AccountId)
ECR_BASE="${ACCOUNT}.dkr.ecr.${REGION}.amazonaws.com"

dnf install -y docker amazon-ecr-credential-helper jq awscli >/dev/null
systemctl enable --now docker

# Compose plugin; falls back to a pinned binary if the distro package is not there.
if ! docker compose version >/dev/null 2>&1; then
  dnf install -y docker-compose-plugin >/dev/null 2>&1 || true
fi
if ! docker compose version >/dev/null 2>&1; then
  mkdir -p /usr/local/lib/docker/cli-plugins
  curl -fsSL "https://github.com/docker/compose/releases/download/v2.27.1/docker-compose-linux-x86_64" \
    -o /usr/local/lib/docker/cli-plugins/docker-compose
  chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
fi
docker compose version

# ECR pulls use the instance role through the credential helper, so the host
# never holds a registry credential (Koda docker.md).
mkdir -p /etc/docker /root/.docker
cat > /etc/docker/config.json <<EOF
{
  "credHelpers": {
    "${ECR_BASE}": "ecr-login"
  }
}
EOF
cat > /root/.docker/config.json <<EOF
{
  "credHelpers": {
    "${ECR_BASE}": "ecr-login"
  }
}
EOF
systemctl restart docker

mkdir -p /opt/ecom/mysql-init

# .env: dev database passwords match the init script; the JWT secret comes from
# SSM. Both fail closed so a boot never falls back to a static string that a
# reader could guess.
MYSQL_ROOT_PASSWORD=$(aws ssm get-parameter --name /ecom/dev/mysql_root_password \
  --with-decryption --region "$REGION" --query Parameter.Value --output text) \
  || { echo "FATAL: /ecom/dev/mysql_root_password not in SSM"; exit 1; }
JWT_SECRET=$(aws ssm get-parameter --name /ecom/dev/jwt_secret \
  --with-decryption --region "$REGION" --query Parameter.Value --output text) \
  || { echo "FATAL: /ecom/dev/jwt_secret not in SSM"; exit 1; }

cat > /opt/ecom/.env <<EOFENV
ECR_BASE=${ECR_BASE}
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
JWT_SECRET=${JWT_SECRET}
ACME_EMAIL=__OWNER_EMAIL__
USER_MS_DB_PW=koda_user_ms_pw
PRODUCT_MS_DB_PW=koda_product_ms_pw
CART_MS_DB_PW=koda_cart_ms_pw
ORDER_MS_DB_PW=koda_order_ms_pw
PAYMENT_MS_DB_PW=koda_payment_ms_pw
SHIPMENT_MS_DB_PW=koda_shipment_ms_pw
NOTIFICATION_MS_DB_PW=koda_notification_ms_pw
TAG_USER_MS=bootstrap
TAG_PRODUCT_MS=bootstrap
TAG_CART_MS=bootstrap
TAG_ORDER_MS=bootstrap
TAG_PAYMENT_MS=bootstrap
TAG_SHIPMENT_MS=bootstrap
TAG_NOTIFICATION_MS=bootstrap
TAG_FRONTEND=bootstrap
EOFENV
chmod 600 /opt/ecom/.env
echo ".env written"

cat > /opt/ecom/docker-compose.yml <<'EOFCOMPOSE'
name: ecom

services:
  mysql:
    image: mysql:8.4
    container_name: ecom-mysql
    restart: unless-stopped
    mem_limit: 768m
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
    volumes:
      - /opt/ecom/mysql-init:/docker-entrypoint-initdb.d:ro
      - ecom-mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 12

  user-ms:
    image: ${ECR_BASE}/ecom/user-ms:${TAG_USER_MS}
    container_name: ecom-user-ms
    restart: unless-stopped
    mem_limit: 280m
    environment:
      MYSQL_HOST: mysql
      MYSQL_USER: koda_user_ms
      MYSQL_PASSWORD: ${USER_MS_DB_PW}
      JWT_SECRET: ${JWT_SECRET}

  product-ms:
    image: ${ECR_BASE}/ecom/product-ms:${TAG_PRODUCT_MS}
    container_name: ecom-product-ms
    restart: unless-stopped
    mem_limit: 280m
    environment:
      MYSQL_HOST: mysql
      MYSQL_USER: koda_product_ms
      MYSQL_PASSWORD: ${PRODUCT_MS_DB_PW}

  cart-ms:
    image: ${ECR_BASE}/ecom/cart-ms:${TAG_CART_MS}
    container_name: ecom-cart-ms
    restart: unless-stopped
    mem_limit: 280m
    environment:
      MYSQL_HOST: mysql
      MYSQL_USER: koda_cart_ms
      MYSQL_PASSWORD: ${CART_MS_DB_PW}

  order-ms:
    image: ${ECR_BASE}/ecom/order-ms:${TAG_ORDER_MS}
    container_name: ecom-order-ms
    restart: unless-stopped
    mem_limit: 280m
    environment:
      MYSQL_HOST: mysql
      MYSQL_USER: koda_order_ms
      MYSQL_PASSWORD: ${ORDER_MS_DB_PW}

  payment-ms:
    image: ${ECR_BASE}/ecom/payment-ms:${TAG_PAYMENT_MS}
    container_name: ecom-payment-ms
    restart: unless-stopped
    mem_limit: 280m
    environment:
      MYSQL_HOST: mysql
      MYSQL_USER: koda_payment_ms
      MYSQL_PASSWORD: ${PAYMENT_MS_DB_PW}

  shipment-ms:
    image: ${ECR_BASE}/ecom/shipment-ms:${TAG_SHIPMENT_MS}
    container_name: ecom-shipment-ms
    restart: unless-stopped
    mem_limit: 280m
    environment:
      MYSQL_HOST: mysql
      MYSQL_USER: koda_shipment_ms
      MYSQL_PASSWORD: ${SHIPMENT_MS_DB_PW}

  notification-ms:
    image: ${ECR_BASE}/ecom/notification-ms:${TAG_NOTIFICATION_MS}
    container_name: ecom-notification-ms
    restart: unless-stopped
    mem_limit: 280m
    environment:
      MYSQL_HOST: mysql
      MYSQL_USER: koda_notification_ms
      MYSQL_PASSWORD: ${NOTIFICATION_MS_DB_PW}

  frontend:
    image: ${ECR_BASE}/ecom/frontend:${TAG_FRONTEND}
    container_name: ecom-frontend
    restart: unless-stopped
    mem_limit: 256m

  caddy:
    image: caddy:2-alpine
    container_name: ecom-caddy
    restart: unless-stopped
    mem_limit: 96m
    ports:
      - "80:80"
      - "443:443"
    environment:
      ACME_EMAIL: ${ACME_EMAIL}
    volumes:
      - /opt/ecom/Caddyfile:/etc/caddy/Caddyfile:ro
      - ecom-caddy-data:/data

volumes:
  ecom-mysql-data:
  ecom-caddy-data:
EOFCOMPOSE
echo "docker-compose.yml written"

cat > /opt/ecom/Caddyfile <<'EOFCADDY'
{
	email {$ACME_EMAIL}
}

ecommerce.kodasolutions.net {
	encode gzip

	@internal path /api/*/internal/*
	respond @internal 404

	@actuator path /api/*/actuator/*
	respond @actuator 404

	handle /api/user/* {
		reverse_proxy user-ms:8040
	}
	handle /api/product/* {
		reverse_proxy product-ms:8050
	}
	handle /api/cart/* {
		reverse_proxy cart-ms:8060
	}
	handle /api/order/* {
		reverse_proxy order-ms:8070
	}
	handle /api/payment/* {
		reverse_proxy payment-ms:8080
	}
	handle /api/shipment/* {
		reverse_proxy shipment-ms:8090
	}
	handle /api/notification/* {
		reverse_proxy notification-ms:8100
	}

	handle {
		reverse_proxy frontend:8080
	}
}
EOFCADDY
echo "Caddyfile written"

cat > /opt/ecom/deploy.sh <<'EOFDEPLOY'
#!/usr/bin/env bash
set -euo pipefail

svc="${1:-}"
sha="${2:-}"
if [[ -z "${svc}" || -z "${sha}" ]]; then
  echo "usage: deploy.sh <service> <sha>" >&2
  exit 2
fi

cd /opt/ecom

var="TAG_${svc//-/_}"
var="${var^^}"

if grep -q "^${var}=" .env; then
  sed -i "s|^${var}=.*|${var}=${sha}|" .env
else
  printf '%s=%s\n' "${var}" "${sha}" >> .env
fi

set -a
source .env
set +a

image="${ECR_BASE}/ecom/${svc}:${sha}"
echo "deploying ${svc} ${sha}"
if ! docker pull "${image}"; then
  echo "pull failed for ${image}; previous container keeps serving" >&2
  exit 1
fi

docker compose -f /opt/ecom/docker-compose.yml up -d "${svc}"
sleep 2
docker compose -f /opt/ecom/docker-compose.yml ps "${svc}" | sed 's/^/  /'
echo "deploy done: ${svc} ${sha}"
EOFDEPLOY
chmod +x /opt/ecom/deploy.sh
echo "deploy.sh written"

cat > /opt/ecom/boot.sh <<'EOFBOOT'
#!/usr/bin/env bash
# Start the stack after a reboot. Missing images (post bootstrap) are fine:
# CI deploys real SHAs after the infra is up.
set +e
for c in mysql caddy frontend user-ms product-ms cart-ms order-ms payment-ms shipment-ms notification-ms; do
  docker compose -f /opt/ecom/docker-compose.yml up -d "$c" || echo "boot: $c not yet available"
  sleep 2
done
EOFBOOT
chmod +x /opt/ecom/boot.sh

# The per-schema least privilege users and databases (SEC-06), the same file
# the local compose uses.
cat > /opt/ecom/mysql-init/001_schemas_and_users.sql <<'EOFDB'
CREATE DATABASE IF NOT EXISTS ecommerce_user;
CREATE DATABASE IF NOT EXISTS ecommerce_product;
CREATE DATABASE IF NOT EXISTS ecommerce_cart;
CREATE DATABASE IF NOT EXISTS ecommerce_order;
CREATE DATABASE IF NOT EXISTS ecommerce_payment;
CREATE DATABASE IF NOT EXISTS ecommerce_shipment;
CREATE DATABASE IF NOT EXISTS ecommerce_notification;

CREATE USER IF NOT EXISTS 'koda_user_ms'@'%' IDENTIFIED BY 'koda_user_ms_pw';
CREATE USER IF NOT EXISTS 'koda_product_ms'@'%' IDENTIFIED BY 'koda_product_ms_pw';
CREATE USER IF NOT EXISTS 'koda_cart_ms'@'%' IDENTIFIED BY 'koda_cart_ms_pw';
CREATE USER IF NOT EXISTS 'koda_order_ms'@'%' IDENTIFIED BY 'koda_order_ms_pw';
CREATE USER IF NOT EXISTS 'koda_payment_ms'@'%' IDENTIFIED BY 'koda_payment_ms_pw';
CREATE USER IF NOT EXISTS 'koda_shipment_ms'@'%' IDENTIFIED BY 'koda_shipment_ms_pw';
CREATE USER IF NOT EXISTS 'koda_notification_ms'@'%' IDENTIFIED BY 'koda_notification_ms_pw';

GRANT ALL PRIVILEGES ON ecommerce_user.* TO 'koda_user_ms'@'%';
GRANT ALL PRIVILEGES ON ecommerce_product.* TO 'koda_product_ms'@'%';
GRANT ALL PRIVILEGES ON ecommerce_cart.* TO 'koda_cart_ms'@'%';
GRANT ALL PRIVILEGES ON ecommerce_order.* TO 'koda_order_ms'@'%';
GRANT ALL PRIVILEGES ON ecommerce_payment.* TO 'koda_payment_ms'@'%';
GRANT ALL PRIVILEGES ON ecommerce_shipment.* TO 'koda_shipment_ms'@'%';
GRANT ALL PRIVILEGES ON ecommerce_notification.* TO 'koda_notification_ms'@'%';

FLUSH PRIVILEGES;
EOFDB
echo "mysql init written"

cat > /etc/systemd/system/ecom-boot.service <<'EOFSVC'
[Unit]
Description=Koda Ecommerce compose stack
After=docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainingAfterExit=yes
ExecStart=/opt/ecom/boot.sh

[Install]
WantedBy=multi-user.target
EOFSVC
systemctl daemon-reload
systemctl enable ecom-boot
echo "systemd unit armed"

/opt/ecom/boot.sh
echo "user-data done"