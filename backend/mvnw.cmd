@REM Maven Wrapper script for Windows
@REM Downloads Maven if not present, then runs it

@echo off
setlocal

set "MAVEN_WRAPPER_PROPERTIES=.mvn\wrapper\maven-wrapper.properties"

for /f "tokens=1,* delims==" %%a in ('findstr "distributionUrl" "%MAVEN_WRAPPER_PROPERTIES%"') do set "DIST_URL=%%b"

for %%i in ("%DIST_URL%") do set "DIST_NAME=%%~ni"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\%DIST_NAME%"

if not exist "%MAVEN_HOME%" (
    echo Downloading Maven from %DIST_URL%...
    mkdir "%USERPROFILE%\.m2\wrapper\dists" 2>nul
    set "TEMP_FILE=%USERPROFILE%\.m2\wrapper\dists\maven.zip"
    powershell -Command "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%USERPROFILE%\.m2\wrapper\dists\maven.zip'"
    powershell -Command "Expand-Archive -Path '%USERPROFILE%\.m2\wrapper\dists\maven.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force"
    del "%USERPROFILE%\.m2\wrapper\dists\maven.zip" 2>nul
)

for /r "%MAVEN_HOME%" %%f in (mvn.cmd) do (
    set "MVN_CMD=%%f"
    goto :found
)
for /r "%USERPROFILE%\.m2\wrapper\dists" %%f in (mvn.cmd) do (
    set "MVN_CMD=%%f"
    goto :found
)

:found
"%MVN_CMD%" %*
