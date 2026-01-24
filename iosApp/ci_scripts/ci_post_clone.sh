#!/bin/sh
set -e

echo "Installing JDK..."
JDK_URL="https://github.com/adoptium/temurin17-binaries/releases/latest/download/OpenJDK17U-jdk_x64_mac_hotspot.tar.gz"
curl -L "$JDK_URL" -o /tmp/jdk.tar.gz
mkdir -p "$CI_WORKSPACE/jdk"
tar -xzf /tmp/jdk.tar.gz -C "$CI_WORKSPACE/jdk" --strip-components=1

export JAVA_HOME="$CI_WORKSPACE/jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

echo "JAVA_HOME=$JAVA_HOME"
java -version

chmod +x ./gradlew
