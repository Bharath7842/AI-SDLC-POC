#!/bin/sh
# Build, tag, and (optionally) push the three application images.
#
# Registry-agnostic by design — no registry is hardcoded. Set REGISTRY
# before running, e.g.:
#   REGISTRY=ghcr.io/<your-org> ./push-images.sh
#   REGISTRY=docker.io/<your-user> ./push-images.sh
#
# By default this script only builds and tags images locally. It will
# NOT run `docker login` or `docker push` on your behalf — see "Manual
# push" below. Pass PUSH=1 to have it push once you've already logged
# in to your chosen registry yourself.
set -eu

REGISTRY="${REGISTRY:-REGISTRY_NOT_SET}"
GIT_SHA=$(git rev-parse --short HEAD 2>/dev/null || echo "local")
PUSH="${PUSH:-0}"

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

build_tag() {
  service="$1"
  context="$2"
  image="${REGISTRY}/port-poc-${service}"

  echo "Building ${service} from ${context} ..."
  docker build -t "${image}:${GIT_SHA}" -t "${image}:latest" "${context}" \
    --label "git.commit=${GIT_SHA}" \
    --label "build.timestamp=$(date -u +%Y-%m-%dT%H:%M:%SZ)"

  echo "Tagged: ${image}:${GIT_SHA} and ${image}:latest"

  if [ "$PUSH" = "1" ]; then
    if [ "$REGISTRY" = "REGISTRY_NOT_SET" ]; then
      echo "REGISTRY is not set — refusing to push. Set REGISTRY=<your-registry> first." >&2
      exit 1
    fi
    echo "Pushing ${image}:${GIT_SHA} ..."
    docker push "${image}:${GIT_SHA}"
    docker push "${image}:latest"
  fi
}

build_tag recipient-service "${ROOT_DIR}/recipient-service"
build_tag donor-service "${ROOT_DIR}/donor-service"
build_tag ui "${ROOT_DIR}/ui"

if [ "$PUSH" != "1" ]; then
  echo
  echo "Images built and tagged locally only (PUSH=0, the default)."
  echo "To push manually once you've chosen a registry:"
  echo "  docker login <registry>"
  echo "  REGISTRY=<registry> PUSH=1 $0"
fi
