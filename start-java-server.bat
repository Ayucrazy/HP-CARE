@echo off
title HP CARE BHOPAL — JAVA BACKEND SERVER
color 0B

echo ==================================================================
echo   HP CARE AUTHORISED SERVICE DESK — JAVA BACKEND LAUNCHER
echo   Partner: HOME COMFORTS · 89 ZONE 2 M.P. NAGAR BHOPAL
echo ==================================================================
echo.

:: Check for Java installation
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Java is not detected in your PATH.
    echo Please ensure Java (JDK 8 or higher) is installed.
    echo.
    echo Download from: https://adoptium.net/ or https://www.oracle.com/java/
    echo.
    pause
    exit /b 1
)

:: Compile HPCareServer.java if needed
echo [*] Checking and Compiling Java Backend Server...
javac -encoding UTF-8 -d bin HPCareServer.java 2>nul
if %errorlevel% neq 0 (
    echo [*] Attempting direct compilation...
    if not exist bin mkdir bin
    javac -encoding UTF-8 -d bin HPCareServer.java
)

if %errorlevel% neq 0 (
    echo [!] Compilation error. Attempting direct execution with Java source...
    java HPCareServer.java
) else (
    echo [*] Java Server compiled successfully!
    echo [*] Starting HP Care Server on Port 8080...
    echo.
    java -cp bin com.hpcare.HPCareServer
)

pause
