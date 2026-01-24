#!/bin/zsh
set -euo pipefail

echo "PWD: $PWD"
echo "CI_PRIMARY_REPOSITORY_PATH: ${CI_PRIMARY_REPOSITORY_PATH:-<empty>}"

# Script liegt in iosApp/ci_scripts -> repo root ist ../..
ROOT="${CI_PRIMARY_REPOSITORY_PATH:-$(cd "$(dirname "$0")/../.." && pwd)}"
echo "ROOT: $ROOT"
cd "$ROOT"

# Writable location INSIDE repo (niemals /jdk)
JDK_DIR="$ROOT/.xcodecloud/jdk17"
mkdir -p "$JDK_DIR"

echo "Installing JDK into: $JDK_DIR"

JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/latest/download/OpenJDK17U-jdk_x64_mac_hotspot.tar.gz"

curl -fL --retry 3 --retry-delay 2 "$JDK_URL" -o /tmp/jdk.tar.gz
ls -lh /tmp/jdk.tar.gz

tar -xzf /tmp/jdk.tar.gz -C "$JDK_DIR" --strip-components=1

export JAVA_HOME="$JDK_DIR/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

echo "JAVA_HOME=$JAVA_HOME"
java -version

chmod +x ./gradlew
./gradlew --version
