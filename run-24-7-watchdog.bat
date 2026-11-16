@echo off
setlocal enabledelayedexpansion

:: Sentinel & DemoBank 24/7 Auto-Restart Watchdog
title Sentinel & DemoBank 24/7 Watchdog

set "BASE_DIR=%~dp0"
cd /d "%BASE_DIR%"

if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_CMD=java"
)

echo ==============================================================
echo   Sentinel + DemoBank 24/7 Self-Healing Watchdog Active
echo   Monitoring ports 9090 (DemoBank) and 8080 (Sentinel)...
echo ==============================================================

:WATCH_LOOP
:: 1. Check DemoBank on port 9090
netstat -ano | findstr /R /C:":9090 .*LISTENING" >nul
if errorlevel 1 (
    echo [%date% %time%] DemoBank (:9090) offline. Restarting...
    start /b "" cmd /c "cd /d "%BASE_DIR%demobank" && "%JAVA_CMD%" -jar target\demobank.jar > "%BASE_DIR%demobank\data\demobank.log" 2>&1"
    timeout /t 2 >nul
)

:: 2. Check Sentinel on port 8080
netstat -ano | findstr /R /C:":8080 .*LISTENING" >nul
if errorlevel 1 (
    echo [%date% %time%] Sentinel (:8080) offline. Restarting...
    start /b "" cmd /c "cd /d "%BASE_DIR%sentinel" && "%JAVA_CMD%" -jar target\sentinel-1.0.jar > "%BASE_DIR%sentinel\data\sentinel.log" 2>&1"
    timeout /t 2 >nul
)

:: Sleep 5 seconds before next health check
timeout /t 5 /nobreak >nul
goto WATCH_LOOP
