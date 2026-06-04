#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "Starting Auction Client from: $PROJECT_ROOT"
echo "Make sure Server is running on port 5000!"
mvn -pl auction-client exec:java
