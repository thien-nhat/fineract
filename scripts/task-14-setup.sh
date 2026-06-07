#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Using Global Search API (Task 14)..."

# 1. Create a client with unique name
UNIQUE_NAME="SearchableClient_$(date +%s)"
fin_curl -X POST "$FIN_URL/clients" -d "{
  \"officeId\": 1,
  \"legalFormId\": 1,
  \"firstname\": \"$UNIQUE_NAME\",
  \"lastname\": \"Task14\",
  \"active\": true,
  \"activationDate\": \"01 June 2026\",
  \"submittedOnDate\": \"01 June 2026\",
  \"dateFormat\": \"dd MMMM yyyy\",
  \"locale\": \"en\"
}" > /dev/null
echo "Created Client: $UNIQUE_NAME"

# 2. Search for the client
echo "Searching for client '$UNIQUE_NAME'..."
fin_curl -G "$FIN_URL/search" --data-urlencode "query=$UNIQUE_NAME" --data-urlencode "resource=clients" | python -m json.tool

# 3. Search for all clients (partial match)
echo "Searching for clients with 'Searchable' in name..."
fin_curl -G "$FIN_URL/search" --data-urlencode "query=Searchable" --data-urlencode "resource=clients" | python -m json.tool
