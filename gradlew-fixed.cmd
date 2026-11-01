@echo off
rem Gradle needs Selector.open(), which needs to create an AF_UNIX socket file under %TEMP%.
rem On this machine that is blocked inside %LOCALAPPDATA%\Temp, so Gradle dies with
rem "Unable to establish loopback connection" before any build work starts.
rem Pointing TEMP at a plain directory sidesteps it. Use this instead of gradlew.bat.

set "TEMP=%USERPROFILE%\.gradle\tmp"
set "TMP=%TEMP%"
if not exist "%TEMP%" mkdir "%TEMP%"

call "%~dp0gradlew.bat" %*
