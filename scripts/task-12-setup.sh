#!/usr/bin/env bash

source "$(dirname "$0")/common.sh"

echo "Running Reports (Task 12)..."

# 1. List available reports
echo "Available Reports:"
fin_curl "$FIN_URL/reports" | python -m json.tool

# 2. Run a specific report (Client Listing)
# Note: report parameters vary by report.
echo "Running 'Client Listing' report..."
fin_curl "$FIN_URL/runreports/Client%20Listing?R_officeId=1" | python -m json.tool
