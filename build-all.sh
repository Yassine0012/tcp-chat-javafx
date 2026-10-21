#!/usr/bin/env bash
# Builds both executable JARs. Requires JDK 21+ and Maven 3.9+.
set -euo pipefail
cd "$(dirname "$0")"
for p in TCPServer TCPClient; do
  echo "=== Building $p ==="
  (cd "$p" && mvn -q clean package)
  echo "-> $p/target/$p.jar"
done
echo "Done. Copy server.properties / client.properties next to the JARs to customise the network settings."
