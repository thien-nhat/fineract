#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Managing Webhooks (Task 11)..."

# 1. Create a Webhook
RESPONSE_HOOK=$(fin_curl -X POST "$FIN_URL/hooks" -d '{
  "name": "Web",
  "displayName": "Practice Hook",
  "isActive": true,
  "config": {
    "Content Type": "json",
    "Payload URL": "https://webhook.site/dummy-url"
  },
  "events": [
    {
      "entityName": "CLIENT",
      "actionName": "CREATE"
    }
  ]
}')
HOOK_ID=$(echo "$RESPONSE_HOOK" | python -c "import sys, json; print(json.load(sys.stdin)['resourceId'])")
echo "Created Webhook with ID: $HOOK_ID"

# 2. Update the Webhook
fin_curl -X PUT "$FIN_URL/hooks/$HOOK_ID" -d '{
  "displayName": "Updated Practice Hook",
  "isActive": true,
  "config": {
    "Content Type": "json",
    "Payload URL": "https://webhook.site/another-dummy-url"
  }
}' > /dev/null
echo "Updated Webhook with ID: $HOOK_ID"

# 3. List Webhooks
echo "Current Webhooks:"
fin_curl "$FIN_URL/hooks" | python -m json.tool
