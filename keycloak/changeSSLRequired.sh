#!/usr/bin/env bash
set -euo pipefail

# Load variables from your .env file if not already exported
export $(grep -v '^#' ../.env | xargs)

docker exec -e KEYCLOAK_ADMIN="$KC_BOOTSTRAP_ADMIN_USERNAME" \
            -e KEYCLOAK_ADMIN_PASSWORD="$KC_BOOTSTRAP_ADMIN_PASSWORD" \
            -e KEYCLOAK_INTERNAL_URL="$KEYCLOAK_INTERNAL_URL" \
            -i vocabulary-api-keycloak-1 bash -lc '
  echo $KEYCLOAK_ADMIN
  echo $KEYCLOAK_ADMIN_PASSWORD
  echo $KEYCLOAK_INTERNAL_URL
  cd /opt/keycloak/bin
  ./kcadm.sh config credentials \
    --server "$KEYCLOAK_INTERNAL_URL" \
    --realm master \
    --user "$KEYCLOAK_ADMIN" \
    --password "$KEYCLOAK_ADMIN_PASSWORD"
  ./kcadm.sh update realms/master -s sslRequired=NONE
  echo "✅ Changed sslRequired using env vars"
'
