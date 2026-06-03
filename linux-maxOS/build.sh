#!/bin/bash
# Script to rebuild entire project from root
# Usage: ./build.sh

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

echo "Building entire Auction System from: $PROJECT_ROOT"
echo ""

mvn clean install

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build complete!"
    echo ""
    echo "Next steps:"
    echo "  - Terminal 1: ./run-server.sh"
    echo "  - Terminal 2: ./run-client.sh"
else
    echo ""
    echo "❌ Build failed!"
fi

