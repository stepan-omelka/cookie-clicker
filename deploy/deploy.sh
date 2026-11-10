#!/usr/bin/env bash
# Builds the backend image locally, deploys it to Cloud Run with hard limits, then deploys the frontend.
set -euo pipefail
cd "$(dirname "$0")/.."
source deploy/config.sh

if [[ ! -f deploy/cloudrun.env.yaml ]]; then
  echo "Missing deploy/cloudrun.env.yaml. Copy deploy/cloudrun.env.example.yaml and fill in Neon." >&2
  exit 1
fi

IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPOSITORY/$SERVICE:$(git rev-parse --short HEAD)"

gcloud auth configure-docker "$REGION-docker.pkg.dev" --quiet
docker buildx build --platform linux/amd64 --tag "$IMAGE" --push backend

gcloud run deploy "$SERVICE" --project "$PROJECT_ID" --region "$REGION" --image "$IMAGE" \
  --allow-unauthenticated --min-instances 0 --max-instances 1 \
  --cpu 1 --memory 512Mi --cpu-throttling --cpu-boost --concurrency 80 --timeout 30 \
  --env-vars-file deploy/cloudrun.env.yaml

(cd frontend && npm ci && npx ng build)
firebase deploy --only hosting --project "$PROJECT_ID"
