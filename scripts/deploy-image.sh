#!/bin/bash
set -euo pipefail

if [ $# -ne 1 ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 1.0.5"
  exit 1
fi

VERSION="$1"

# Directory where the pom.xml is located (one level above this script)
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

LOCAL_IMAGE="vocabulary/vocabulary-api"
REMOTE_IMAGE="egch/vocabulary-api:${VERSION}"

echo "Using project directory: $PROJECT_DIR"

cd "$PROJECT_DIR"

echo "Building Spring Boot image..."
mvn spring-boot:build-image -DskipTests

echo "Tagging image ${LOCAL_IMAGE} as ${REMOTE_IMAGE}..."
docker tag "${LOCAL_IMAGE}" "${REMOTE_IMAGE}"

echo "Pushing image ${REMOTE_IMAGE}..."
docker push "${REMOTE_IMAGE}"

echo "Done: ${REMOTE_IMAGE}"