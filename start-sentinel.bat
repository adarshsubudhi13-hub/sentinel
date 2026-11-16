@echo off
if exist "%JAVA_HOME%\bin\java.exe" (
    "%JAVA_HOME%\bin\java.exe" -jar target\sentinel-1.0.jar
) else (
    java -jar target\sentinel-1.0.jar
)
