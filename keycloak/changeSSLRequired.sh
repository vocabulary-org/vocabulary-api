#!/usr/bin/env bash
set -euo pipefail

# Load variables from your .env file if not already exported
export $(grep -v '^#' ../.env | xargs)

docker exec -e KEYCLOAK_ADMIN="$KC_BOOTSTRAP_ADMIN_USERNAME" \
            -e KEYCLOAK_ADMIN_PASSWORD="$KC_BOOTSTRAP_ADMIN_PASSWORD" \
            -i vocabulary-api-keycloak-1 bash -lc '
  echo $KEYCLOAK_ADMIN
  echo $KEYCLOAK_ADMIN_PASSWORD
  cd /opt/keycloak/bin
  ./kcadm.sh config credentials \
    --server http://127.0.0.1:8080 \
    --realm master \
    --user "$KEYCLOAK_ADMIN" \
    --password "$KEYCLOAK_ADMIN_PASSWORD"
  ./kcadm.sh update realms/master -s sslRequired=NONE
  echo "✅ Changed sslRequired using env vars"
'
