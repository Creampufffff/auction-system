@echo off
REM Script to rebuild entire project from root
REM Usage: build.bat

setlocal enabledelayedexpansion
cd /d "%~dp0\.."

echo Building entire Auction System from: %cd%
echo.

mvn clean install

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build complete!
    echo.
    echo Next steps:
    echo   - Terminal 1: run-server.bat (or .\run-server.ps1)
    echo   - Terminal 2: run-client.bat (or .\run-client.ps1)
) else (
    echo Build failed!
)

pause

