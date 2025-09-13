#!/bin/bash

TOKEN=$(curl -X POST \
  http://localhost:18081/realms/vocabulary/protocol/openid-connect/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d username=admin \
  -d password=admin \
  -d grant_type=password \
  -d client_id=vocabulary-rest-api \
  | jq -r .access_token)

echo $TOKEN | pbcopy