#!/bin/bash
# ==============================================================================
# CampusFlow Mobile Web Application Build & Test Script
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

echo "1. Creating directories..."
mkdir -p out web_data backups screenshots

echo "2. Compiling Java sources..."
find src -name "*.java" > /tmp/sources.txt
javac -encoding UTF-8 -d out @/tmp/sources.txt
if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "3. Compiling test suite..."
javac -encoding UTF-8 -cp out -d out test/WebIntegrationTest.java
if [ $? -ne 0 ]; then
    echo "Test compilation failed!"
    exit 1
fi

echo "4. Running automated integration tests..."
java -cp out test.WebIntegrationTest
if [ $? -ne 0 ]; then
    echo "Tests failed!"
    exit 1
fi

echo "5. Building standalone CampusFlowWeb.jar..."
mkdir -p /tmp/jar_staging
cp -r out/* /tmp/jar_staging/
mkdir -p /tmp/jar_staging/static
cp -r static/* /tmp/jar_staging/static/

cat << 'EOF' > /tmp/web_manifest.mf
Manifest-Version: 1.0
Main-Class: web.CampusFlowWebServer
Specification-Title: CampusFlow Mobile Web Application
Specification-Version: 1.0
Implementation-Title: CampusFlow
Implementation-Version: 1.0
Built-By: CampusFlow Team
Created-By: 11.0.2 (Ubuntu)
EOF

JAR_BIN=$(which jar 2>/dev/null || echo "/usr/lib/jvm/jdk-11/bin/jar")
$JAR_BIN cfm CampusFlowWeb.jar /tmp/web_manifest.mf -C /tmp/jar_staging .
rm -rf /tmp/jar_staging /tmp/web_manifest.mf /tmp/sources.txt

echo "================================================================="
echo " Build successful! CampusFlowWeb.jar created."
echo "================================================================="
