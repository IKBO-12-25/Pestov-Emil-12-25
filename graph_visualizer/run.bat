@echo off
setlocal
set SCRIPT_DIR=%~dp0
call "%SCRIPT_DIR%gradlew.bat" -q -p "%SCRIPT_DIR%" run %*
exit /b %ERRORLEVEL%
