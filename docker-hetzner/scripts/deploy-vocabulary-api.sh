#!/bin/bash
set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 1.0.5"
  exit 1
fi

VERSION=$1
IMAGE="egch/vocabulary-api:${VERSION}"
COMPOSE_FILE="../docker-compose-vocabulary-api.yaml"

echo "Deploying vocabulary-api version ${VERSION}"

echo "Stopping compose stack..."
docker compose -f ${COMPOSE_FILE} down

echo "Removing image ${IMAGE} if present..."
docker image rm ${IMAGE} 2>/dev/null || true

echo "Pulling image ${IMAGE}..."
docker pull ${IMAGE}

echo "Starting compose stack..."
IMAGE_TAG=${VERSION} docker compose -f ${COMPOSE_FILE} up -d

echo "Deployment completed."