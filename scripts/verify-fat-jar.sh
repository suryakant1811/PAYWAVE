#!/usr/bin/env bash
# Ensure a module produced an executable Spring Boot fat JAR (not a ~15 KiB thin JAR).
set -euo pipefail
JAR="${1:-}"
MIN_BYTES="${2:-20000000}"
if [[ -z "$JAR" || ! -f "$JAR" ]]; then
  echo "usage: $0 path/to/service-1.0.0.jar" >&2
  exit 1
fi
SIZE=$(wc -c <"$JAR" | tr -d ' ')
if [[ "$SIZE" -lt "$MIN_BYTES" ]]; then
  echo "FAIL $JAR is only ${SIZE} bytes — not a fat JAR." >&2
  echo "Run: mvn -pl <module> -am clean package -DskipTests" >&2
  echo "Do not run spring-boot:repackage alone — it fails after package already repackaged." >&2
  echo "Then check for BOOT-INF/lib inside the JAR." >&2
  exit 1
fi
if ! unzip -l "$JAR" 2>/dev/null | grep -q 'BOOT-INF/lib/'; then
  echo "FAIL $JAR has no BOOT-INF/lib — spring-boot repackage did not run." >&2
  exit 1
fi
echo "OK   fat JAR $JAR (${SIZE} bytes)"