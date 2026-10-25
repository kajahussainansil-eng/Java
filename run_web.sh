#!/bin/bash
# ==============================================================================
# CampusFlow Mobile Web Application Launcher
# "Your Campus. Connected."
# ==============================================================================

PORT=${1:-8080}
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

echo "================================================================="
echo " Starting CampusFlow Mobile Web Server on port $PORT..."
echo "================================================================="

if [ -f "CampusFlowWeb.jar" ]; then
    java -jar CampusFlowWeb.jar "$PORT"
else
    java -cp "out" web.CampusFlowWebServer "$PORT"
fi
