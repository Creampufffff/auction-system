#!/bin/bash
# Script to run Auction Client from project root
# Usage: ./run-client.sh

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

echo "Starting Auction Client from: $PROJECT_ROOT"
echo "Make sure Server is running on port 5000!"
echo ""

mvn -pl auction-client exec:java

