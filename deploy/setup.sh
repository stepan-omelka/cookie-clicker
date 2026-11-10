#!/usr/bin/env bash
# One-time setup: APIs, image registry with cleanup, billing kill switch.
set -euo pipefail
cd "$(dirname "$0")"
source ./config.sh

run() { echo "+ $*"; "$@"; }

BILLING_ACCOUNT=$(gcloud billing projects describe "$PROJECT_ID" --format 'value(billingAccountName)')
BILLING_ACCOUNT="${BILLING_ACCOUNT#billingAccounts/}"
if [[ -z "$BILLING_ACCOUNT" ]]; then
  echo "Project $PROJECT_ID has no billing account. Upgrade it to the Blaze plan first." >&2
  exit 1
fi
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format 'value(projectNumber)')
CURRENCY=$(gcloud billing accounts describe "$BILLING_ACCOUNT" --format 'value(currencyCode)' 2>/dev/null || true)
CURRENCY="${CURRENCY:-USD}"
SA="$KILL_SWITCH@$PROJECT_ID.iam.gserviceaccount.com"

run gcloud services enable --project "$PROJECT_ID" \
  run.googleapis.com artifactregistry.googleapis.com cloudbilling.googleapis.com \
  billingbudgets.googleapis.com pubsub.googleapis.com cloudfunctions.googleapis.com \
  cloudbuild.googleapis.com eventarc.googleapis.com

if ! gcloud artifacts repositories describe "$REPOSITORY" --location "$REGION" --project "$PROJECT_ID" >/dev/null 2>&1; then
  run gcloud artifacts repositories create "$REPOSITORY" --repository-format docker \
    --location "$REGION" --project "$PROJECT_ID"
fi
run gcloud artifacts repositories set-cleanup-policies "$REPOSITORY" --location "$REGION" \
  --project "$PROJECT_ID" --policy cleanup-policy.json --no-dry-run

if ! gcloud pubsub topics describe "$TOPIC" --project "$PROJECT_ID" >/dev/null 2>&1; then
  run gcloud pubsub topics create "$TOPIC" --project "$PROJECT_ID"
fi
if ! gcloud iam service-accounts describe "$SA" --project "$PROJECT_ID" >/dev/null 2>&1; then
  run gcloud iam service-accounts create "$KILL_SWITCH" --display-name "Billing kill switch" \
    --project "$PROJECT_ID"
fi
run gcloud projects add-iam-policy-binding "$PROJECT_ID" --member "serviceAccount:$SA" \
  --role roles/billing.projectManager --condition None --quiet >/dev/null

run gcloud functions deploy "$KILL_SWITCH" --gen2 --runtime python312 --region "$REGION" \
  --project "$PROJECT_ID" --source kill-switch --entry-point stop_billing \
  --trigger-topic "$TOPIC" --service-account "$SA" --trigger-service-account "$SA" \
  --memory 256Mi --max-instances 1 --set-env-vars "TARGET_PROJECT=$PROJECT_ID,KILL_AT=$KILL_AT"
run gcloud functions add-invoker-policy-binding "$KILL_SWITCH" --region "$REGION" \
  --project "$PROJECT_ID" --member "serviceAccount:$SA"

BUDGET_NAME="$PROJECT_ID kill switch"
EXISTING=$(gcloud billing budgets list --billing-account "$BILLING_ACCOUNT" --project "$PROJECT_ID" \
  --filter "displayName=\"$BUDGET_NAME\"" --format 'value(name)')
if [[ -z "$EXISTING" ]]; then
  run gcloud billing budgets create --billing-account "$BILLING_ACCOUNT" --project "$PROJECT_ID" \
    --display-name "$BUDGET_NAME" --budget-amount "${BUDGET_AMOUNT}${CURRENCY}" \
    --filter-projects "projects/$PROJECT_NUMBER" --credit-types-treatment include-all-credits \
    --threshold-rule percent=0.5 --threshold-rule percent=0.9 --threshold-rule percent=1.0 \
    --notifications-rule-pubsub-topic "projects/$PROJECT_ID/topics/$TOPIC"
fi

echo
echo "Kill switch is on: billing for $PROJECT_ID is disabled once net cost reaches $KILL_AT $CURRENCY."
echo "Next: create the Cloud Run spend cap in the console (deploy/README.md, step 4)."
