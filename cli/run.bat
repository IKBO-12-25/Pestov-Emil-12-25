@echo off
setlocal enabledelayedexpansion

set "OUT_DIR=build\classes\java\main"
if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

set "SOURCES="
for /r "src\main\java" %%f in (*.java) do (
    set "SOURCES=!SOURCES! "%%f""
)

javac -encoding UTF-8 -d "%OUT_DIR%" %SOURCES%
if errorlevel 1 exit /b %errorlevel%

java -cp "%OUT_DIR%" com.r.Main %*