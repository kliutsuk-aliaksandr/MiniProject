@echo off
setlocal
set "GRADLE_VERSION=8.13"
if not defined GRADLE_USER_HOME set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "INSTALL_ROOT=%GRADLE_USER_HOME%\miniproject"
set "GRADLE_HOME=%INSTALL_ROOT%\gradle-%GRADLE_VERSION%"

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
    if not exist "%INSTALL_ROOT%" mkdir "%INSTALL_ROOT%"
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $zip=Join-Path $env:GRADLE_USER_HOME 'miniproject\gradle-%GRADLE_VERSION%-bin.zip'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -LiteralPath $zip -DestinationPath (Split-Path $zip) -Force"
    if errorlevel 1 exit /b 1
)

call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
