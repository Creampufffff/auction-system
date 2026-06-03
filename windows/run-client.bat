@echo off
REM Script to run Auction Client from project root
REM Usage: run-client.bat

setlocal enabledelayedexpansion
cd /d "%~dp0\.."

echo Starting Auction Client from: %cd%
echo Make sure Server is running on port 5000!
echo.

mvn -pl auction-client exec:java

pause

