#!/usr/bin/env bash
# Deploy one compose service at a specific image SHA. Pulls before touching the
# running container, so a missing image or an unreachable registry leaves the
# current version serving traffic. Called by GitHub Actions over SSM.
set -euo pipefail

svc="${1:-}"
sha="${2:-}"
if [[ -z "${svc}" || -z "${sha}" ]]; then
  echo "usage: deploy.sh <service> <sha>" >&2
  exit 2
fi

cd /opt/ecom

# TAG_USER_MS, TAG_PRODUCT_MS, ... from the service name.
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