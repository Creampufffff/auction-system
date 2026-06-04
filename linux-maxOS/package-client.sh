#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-1.0.0}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_ROOT="$PROJECT_ROOT/build/client-package"
INPUT_DIR="$BUILD_ROOT/input"
DIST_DIR="$PROJECT_ROOT/dist"
APP_NAME="AuctionClient"

case "$(uname -s)" in
  Linux*) OS_NAME="linux" ;;
  Darwin*) OS_NAME="macos" ;;
  *) echo "Unsupported operating system: $(uname -s)" >&2; exit 1 ;;
esac

case "$(uname -m)" in
  x86_64|amd64) ARCH_NAME="x64" ;;
  arm64|aarch64) ARCH_NAME="arm64" ;;
  *) ARCH_NAME="$(uname -m)" ;;
esac

ARCHIVE_BASE="$APP_NAME-$OS_NAME-$ARCH_NAME-$VERSION"

rm -rf "$BUILD_ROOT" "$DIST_DIR/$APP_NAME" "$DIST_DIR/$APP_NAME.app"
mkdir -p "$INPUT_DIR" "$DIST_DIR"

cd "$PROJECT_ROOT"
mvn -B -pl auction-client -am install -DskipTests
mvn -B -pl auction-client dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory="$INPUT_DIR"
cp auction-client/target/auction-client-1.0-SNAPSHOT.jar "$INPUT_DIR/"

jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$VERSION" \
  --input "$INPUT_DIR" \
  --dest "$DIST_DIR" \
  --main-jar auction-client-1.0-SNAPSHOT.jar \
  --main-class com.auction.ClientLauncher

if [[ "$OS_NAME" == "macos" ]]; then
  cp auction-client/.env.example "$DIST_DIR/$APP_NAME.app/Contents/MacOS/.env.example"
  ditto -c -k --sequesterRsrc --keepParent \
    "$DIST_DIR/$APP_NAME.app" "$DIST_DIR/$ARCHIVE_BASE.zip"
  echo "Created portable client: $DIST_DIR/$ARCHIVE_BASE.zip"
else
  cp auction-client/.env.example "$DIST_DIR/$APP_NAME/bin/.env.example"
  tar -C "$DIST_DIR" -czf "$DIST_DIR/$ARCHIVE_BASE.tar.gz" "$APP_NAME"
  echo "Created portable client: $DIST_DIR/$ARCHIVE_BASE.tar.gz"
fi
