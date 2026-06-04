#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "Building entire Auction System from: $PROJECT_ROOT"
mvn clean install

echo
echo "Build complete!"
echo "Next steps:"
echo "  - Terminal 1: ./linux-maxOS/run-server.sh"
echo "  - Terminal 2: ./linux-maxOS/run-client.sh"
