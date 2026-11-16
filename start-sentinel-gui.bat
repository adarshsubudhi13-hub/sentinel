@echo off
setlocal
if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_CMD=java"
)
if exist "sentinel" cd sentinel
echo Launching Sentinel Autonomous SOC Java Swing Desktop GUI Console...
"%JAVA_CMD%" -cp target\sentinel-1.0.jar com.sentinel.gui.SentinelSwingApp
