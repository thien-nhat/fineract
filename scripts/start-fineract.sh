#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

export FINERACT_DEFAULT_TENANTDB_PORT="${FINERACT_DEFAULT_TENANTDB_PORT:-5432}"
export FINERACT_HIKARI_DRIVER_SOURCE_CLASS_NAME="${FINERACT_HIKARI_DRIVER_SOURCE_CLASS_NAME:-org.postgresql.Driver}"
export FINERACT_HIKARI_JDBC_URL="${FINERACT_HIKARI_JDBC_URL:-jdbc:postgresql://localhost:${FINERACT_DEFAULT_TENANTDB_PORT}/fineract_tenants}"
export FINERACT_HIKARI_USERNAME="${FINERACT_HIKARI_USERNAME:-postgres}"
export FINERACT_HIKARI_PASSWORD="${FINERACT_HIKARI_PASSWORD:-skdcnwauicn2ucnaecasdsajdnizucawencascdca}"
export FINERACT_DEFAULT_TENANTDB_UID="${FINERACT_DEFAULT_TENANTDB_UID:-postgres}"
export FINERACT_DEFAULT_TENANTDB_PWD="${FINERACT_DEFAULT_TENANTDB_PWD:-$FINERACT_HIKARI_PASSWORD}"

exec mvn -f fineract-provider/pom.xml -Dmaven.test.skip=true spring-boot:run