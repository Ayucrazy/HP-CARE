@echo off
title HP CARE SERVICE DESK - ONE-CLICK LAUNCHER
color 0B
cls
echo ===================================================================
echo   HP CARE AUTHORISED SERVICE DESK (HOME COMFORTS BHOPAL)
echo   One-Click Multi-Laptop Central Application Launcher
echo ===================================================================
echo.
echo [1/3] Checking system environment...
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [2/3] Starting Standalone Java Central Server...
    javac HPCareServer.java DatabaseManager.java 2>nul
    start "" cmd /k "title HP CARE JAVA SERVER && java -cp . com.hpcare.HPCareServer"
    goto OPEN_BROWSER
)

where python >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [2/3] Starting Standalone Python Central Server...
    start "" cmd /k "title HP CARE PYTHON SERVER && python server.py"
    goto OPEN_BROWSER
)

echo [2/3] Starting in Standalone Browser Mode...
start index.html
goto DONE

:OPEN_BROWSER
echo [3/3] Opening HP Care Service Desk in browser...
timeout /t 2 /nobreak >nul
start http://localhost:8080

:DONE
echo.
echo ===================================================================
echo   SUCCESS: HP CARE SERVICE DESK IS RUNNING!
echo   Main URL (This PC):    http://localhost:8080
echo   Direct Web App File:   index.html
echo ===================================================================
timeout /t 5
