#!/bin/bash
# Script to run Auction Server from project root
# Usage: ./run-server.sh

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

echo "Starting Auction Server from: $PROJECT_ROOT"
echo "Database: $(grep 'AUCTION_DB_URL' .env)"
echo ""

mvn -pl auction-server exec:java

