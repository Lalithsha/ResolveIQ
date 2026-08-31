#!/usr/bin/env bash
set -euo pipefail

API_ROOT="${RESOLVEIQ_SEED_API_ROOT:-http://localhost:8080/api/v1}"
KM_EMAIL="${RESOLVEIQ_SEED_KM_EMAIL:-elena.rostova@resolveiq.local}"
KM_PASSWORD="${RESOLVEIQ_SEED_KM_PASSWORD:-ResolveIQ2026!}"
ARTICLE_TITLE="Payment Reconciliation & Duplicate Charge Handling"

command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }
command -v jq >/dev/null || { echo "jq is required" >&2; exit 1; }

login_payload="$(jq -n --arg email "${KM_EMAIL}" --arg password "${KM_PASSWORD}" '{email:$email,password:$password}')"
login_response="$(curl --fail --silent --show-error -H 'Content-Type: application/json' -d "${login_payload}" "${API_ROOT}/auth/login")"
access_token="$(jq -r '.accessToken // empty' <<<"${login_response}")"
[[ -n "${access_token}" ]] || { echo "Knowledge seed login returned no access token" >&2; exit 1; }

auth_header="Authorization: Bearer ${access_token}"
documents="$(curl --fail --silent --show-error -H "${auth_header}" "${API_ROOT}/knowledge/articles")"
document_id="$(jq -r --arg title "${ARTICLE_TITLE}" '.[] | select(.title == $title) | .id' <<<"${documents}" | head -n 1)"

if [[ -z "${document_id}" ]]; then
  content='When a customer reports duplicate charges on an account or a gateway timeout, locate both transactions in the payment processor. Verify that the amount, payment method fingerprint, merchant and five-minute authorization window match. Never request or expose full card data. If two captured charges exist, follow the approved duplicate-refund workflow. If one entry is only a pending authorization, explain the bank release window and do not create an unnecessary refund. Record the processor transaction identifier, update the invoice state, and tell the customer that bank clearance normally takes 3-5 business days.'
  create_payload="$(jq -n --arg title "${ARTICLE_TITLE}" --arg content "${content}" '{title:$title,category:"BILLING",product:"Billing Core",language:"en",summary:"Evidence-safe duplicate charge and pending authorization handling.",content:$content}')"
  created="$(curl --fail --silent --show-error -H "${auth_header}" -H 'Content-Type: application/json' -d "${create_payload}" "${API_ROOT}/knowledge/articles")"
  document_id="$(jq -r '.id' <<<"${created}")"
  echo "Created knowledge draft ${document_id}"
fi

versions="$(curl --fail --silent --show-error -H "${auth_header}" "${API_ROOT}/knowledge/articles/${document_id}/versions")"
published_id="$(jq -r '.[] | select(.status == "PUBLISHED") | .id' <<<"${versions}" | head -n 1)"
if [[ -n "${published_id}" ]]; then
  echo "Knowledge article is already published; ingestion is idempotent."
  exit 0
fi

review_id="$(jq -r '.[] | select(.status == "IN_REVIEW") | .id' <<<"${versions}" | head -n 1)"
if [[ -z "${review_id}" ]]; then
  draft_id="$(jq -r '.[] | select(.status == "DRAFT") | .id' <<<"${versions}" | head -n 1)"
  [[ -n "${draft_id}" ]] || { echo "No publishable draft exists for ${document_id}" >&2; exit 1; }
  submitted="$(curl --fail --silent --show-error -X POST -H "${auth_header}" "${API_ROOT}/knowledge/articles/${document_id}/versions/${draft_id}/submit")"
  review_id="$(jq -r '.id' <<<"${submitted}")"
fi

publish_payload='{"note":"Approved reproducible portfolio seed"}'
curl --fail --silent --show-error -X POST -H "${auth_header}" -H 'Content-Type: application/json' -d "${publish_payload}" "${API_ROOT}/knowledge/articles/${document_id}/versions/${review_id}/publish" >/dev/null

search_payload='{"queryText":"duplicate charge invoice billing dispute credit card","topK":5,"category":"BILLING"}'
search_response="$(curl --fail --silent --show-error -H "${auth_header}" -H 'Content-Type: application/json' -d "${search_payload}" "${API_ROOT}/retrieval/search")"
result_count="$(jq '.citations | length' <<<"${search_response}")"
[[ "${result_count}" -gt 0 ]] || { echo "Published article failed the retrieval verification" >&2; exit 1; }
echo "Published and retrieval-verified knowledge article ${document_id}."
