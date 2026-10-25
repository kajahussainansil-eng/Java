@echo off
REM ==============================================================================
REM CampusFlow Mobile Web Application Launcher (Windows)
REM "Your Campus. Connected."
REM ==============================================================================

set PORT=%1
if "%PORT%"=="" set PORT=8080

cd /d "%~dp0"

echo =================================================================
echo  Starting CampusFlow Mobile Web Server on port %PORT%...
echo =================================================================

if exist "CampusFlowWeb.jar" (
    java -jar CampusFlowWeb.jar %PORT%
) else (
    java -cp "out" web.CampusFlowWebServer %PORT%
)
pause
