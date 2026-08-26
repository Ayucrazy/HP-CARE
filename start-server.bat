@echo off
title HP CARE BHOPAL - Local Network Server
color 0B
cls
echo =========================================================================
echo       HP CARE AUTHORISED SERVICE DESK - LOCAL NETWORK SERVER
echo       HOME COMFORTS - 89 ZONE 2 M.P. NAGAR BHOPAL
echo =========================================================================
echo.
echo Starting HP Care Local Server on Port 8080...
echo.

where python >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    python server.py
    goto end
)

where py >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    py server.py
    goto end
)

echo [INFO] Python not found, starting native PowerShell Web Server...
powershell -ExecutionPolicy Bypass -Command "$port = 8080; $ip = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.InterfaceAlias -notlike '*Loopback*' -and $_.IPAddress -notlike '169.*' } | Select-Object -First 1).IPAddress; Write-Host '========================================================================='; Write-Host '  HP CARE SERVICE DESK - LIVE ON LOCAL NETWORK'; Write-Host '========================================================================='; Write-Host ''; Write-Host '  On THIS laptop:       http://localhost:8080'; Write-Host ('  On OTHER laptops:     http://' + $ip + ':8080'); Write-Host ''; Write-Host '========================================================================='; Start-Process ('http://localhost:' + $port); python -m http.server $port"

:end
pause
