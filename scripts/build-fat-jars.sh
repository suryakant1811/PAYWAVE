#!/usr/bin/env bash
# Build runnable Spring Boot fat JARs for every deployable service (incl. gateway UI).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
MODULES=(account-service wallet-service payment-service settlement-service api-gateway web-ui-service)
VERIFY="${ROOT}/scripts/verify-fat-jar.sh"
[[ -x "$VERIFY" ]] || chmod +x "$VERIFY"

for module in "${MODULES[@]}"; do
  echo "==> $module"
  mvn -pl "$module" -am package -DskipTests
  # Idempotent if repackage already ran during package
  mvn -q -pl "$module" spring-boot:repackage
  jar="${ROOT}/${module}/target/${module}-1.0.0.jar"
  "$VERIFY" "$jar"
done

echo "All fat JARs OK"