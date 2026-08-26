#!/usr/bin/env bash
# HP CARE BHOPAL — JAVA BACKEND SERVER LAUNCHER

echo "=================================================================="
echo "  HP CARE AUTHORISED SERVICE DESK — JAVA BACKEND LAUNCHER"
echo "  Partner: HOME COMFORTS · 89 ZONE 2 M.P. NAGAR BHOPAL"
echo "=================================================================="
echo ""

if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed or not in PATH."
    echo "Please install OpenJDK 17 or higher: https://adoptium.net/"
    exit 1
fi

mkdir -p bin
javac -encoding UTF-8 -d bin HPCareServer.java
if [ $? -eq 0 ]; then
    echo "[*] Java Server compiled successfully! Starting on port 8080..."
    java -cp bin com.hpcare.HPCareServer
else
    echo "[*] Running with source execution..."
    java HPCareServer.java
fi
