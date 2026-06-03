@echo off
REM Script to run Auction Server from project root
REM Usage: run-server.bat

setlocal enabledelayedexpansion
cd /d "%~dp0\.."

echo Starting Auction Server from: %cd%
echo.

mvn -pl auction-server exec:java

pause

