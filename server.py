#!/usr/bin/env python3
"""
HP CARE BHOPAL — CENTRAL NETWORK & CLOUD SERVER (Zero-dependency)
Location: HOME COMFORTS · 89 ZONE 2 M.P. NAGAR BHOPAL
Run this to allow all laptops and devices to connect, sync, and print in real-time.
"""

import http.server
import socketserver
import socket
import os
import json
import sys
import uuid
import re
from datetime import datetime

PORT = int(os.environ.get("PORT", 8080))
DIRECTORY = os.path.dirname(os.path.abspath(__file__))
DB_FILE = os.path.join(DIRECTORY, "hp_cases_database.json")
UPLOADS_DIR = os.path.join(DIRECTORY, "uploads")

os.makedirs(UPLOADS_DIR, exist_ok=True)

def get_local_ip():
    """Get the local WiFi / LAN IP address of this Server Laptop."""
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
        ip = s.getsockname()[0]
    except Exception:
        ip = '127.0.0.1'
    finally:
        s.close()
    return ip

def generate_next_case_no(cases, is_quote=False):
    if is_quote:
        max_num = 20261
        for c in cases:
            no_str = str(c.get("quoteNo", "") or c.get("caseNo", "") or c.get("id", ""))
            m = re.search(r"Q(\d+)", no_str, re.I)
            if m:
                try:
                    val = int(m.group(1))
                    if val > max_num: max_num = val
                except ValueError: pass
        return f"Q{max_num + 1}"
    else:
        max_num = 1805
        for c in cases:
            no_str = str(c.get("caseNo", "") or c.get("id", ""))
            m = re.search(r"HC[ -]BPL[ -]?(\d+)", no_str, re.I)
            if m:
                try:
                    val = int(m.group(1))
                    if val > max_num: max_num = val
                except ValueError: pass
        return f"HC BPL {max_num + 1}"

class HPCareHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def end_cors(self, code=200, ctype="application/json"):
        self.send_response(code)
        self.send_header('Content-type', f'{ctype}; charset=utf-8')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Requested-With, X-Case-Version')
        self.send_header('Cache-Control', 'no-cache, no-store, must-revalidate')
        self.end_headers()

    def do_OPTIONS(self):
        self.send_response(204)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Requested-With, X-Case-Version')
        self.end_headers()

    def do_GET(self):
        clean_path = self.path.split("?")[0]
        if clean_path == "/api/cases":
            self.end_cors(200)
            if os.path.exists(DB_FILE):
                with open(DB_FILE, "rb") as f:
                    self.wfile.write(f.read())
            else:
                self.wfile.write(b"[]")
            return
        elif clean_path == "/api/cases/export":
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Content-Disposition', 'attachment; filename="hp_care_backup.json"')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            if os.path.exists(DB_FILE):
                with open(DB_FILE, "rb") as f:
                    self.wfile.write(f.read())
            else:
                self.wfile.write(b"[]")
            return
        elif clean_path == "/api/health":
            self.end_cors(200)
            self.wfile.write(b'{"status":"UP"}')
            return
        elif clean_path == "/api/stats":
            self.end_cors(200)
            count = 0
            if os.path.exists(DB_FILE):
                try:
                    with open(DB_FILE, "r", encoding="utf-8") as f:
                        count = len(json.load(f))
                except Exception: pass
            resp = json.dumps({"status": "online", "casesCount": count, "server": "HP Care Python v3.0"})
            self.wfile.write(resp.encode("utf-8"))
            return

        return super().do_GET()

    def do_POST(self):
        clean_path = self.path.split("?")[0]
        length = int(self.headers.get('content-length', 0))
        body = self.rfile.read(length)

        if clean_path == "/api/cases":
            try:
                data = json.loads(body.decode('utf-8'))
                existing = []
                if os.path.exists(DB_FILE):
                    try:
                        with open(DB_FILE, "r", encoding="utf-8") as f:
                            existing = json.load(f)
                    except Exception: existing = []

                if isinstance(data, list):
                    # Bulk sync
                    with open(DB_FILE, "w", encoding="utf-8") as f:
                        json.dump(data, f, indent=2, ensure_ascii=False)
                    self.end_cors(200)
                    self.wfile.write(b'{"status":"ok","message":"Database synced"}')
                    return
                elif isinstance(data, dict):
                    # Single case atomic upsert
                    target_id = str(data.get("id") or data.get("caseNo") or "").strip()
                    is_quote = (str(data.get("obligation", "")).lower() == "quotation" or 
                                str(data.get("caseType", "")).lower() == "quotation")
                    
                    if not target_id or target_id == "null":
                        target_id = generate_next_case_no(existing, is_quote)
                        data["id"] = target_id
                        data["caseNo"] = target_id

                    # Upsert with version check
                    found_idx = -1
                    for idx, c in enumerate(existing):
                        if str(c.get("id")) == target_id or str(c.get("caseNo")) == target_id:
                            found_idx = idx
                            break

                    client_version = int(self.headers.get("X-Case-Version") or data.get("version") or 1)
                    if found_idx >= 0:
                        server_version = int(existing[found_idx].get("version") or 1)
                        if client_version < server_version:
                            self.end_cors(409)
                            self.wfile.write(json.dumps({
                                "error": "CONFLICT",
                                "message": f"Conflict: Case {target_id} was modified on another terminal (v{server_version}). Please refresh."
                            }).encode("utf-8"))
                            return
                        data["version"] = server_version + 1
                        existing[found_idx] = data
                    else:
                        data["version"] = 1
                        existing.insert(0, data)

                    with open(DB_FILE, "w", encoding="utf-8") as f:
                        json.dump(existing, f, indent=2, ensure_ascii=False)

                    self.end_cors(200)
                    self.wfile.write(json.dumps({"status": "ok", "case": data, "caseNo": target_id}).encode("utf-8"))
                    return
            except Exception as e:
                self.end_cors(500)
                self.wfile.write(json.dumps({"error": True, "message": str(e)}).encode('utf-8'))
                return

        elif clean_path == "/api/auth/login" or clean_path == "/api/login":
            try:
                payload = json.loads(body.decode('utf-8'))
                user = payload.get("user", "")
                pwd = payload.get("pass", "")
                if user == "anchit" and pwd == "anchitsir":
                    resp = {"authenticated": True, "user": "anchit", "name": "Anchit Sir (Owner / Super Admin)", "role": "SUPER_ADMIN", "token": f"HP-{uuid.uuid4()}"}
                elif user == "ayush" and pwd == "ayush":
                    resp = {"authenticated": True, "user": "ayush", "name": "Ayush Sharma (Admin Desk)", "role": "ADMIN", "token": f"HP-{uuid.uuid4()}"}
                elif user == "danish" and pwd == "danish":
                    resp = {"authenticated": True, "user": "danish", "name": "Danish (Service Engineer)", "role": "SERVICE_ENGINEER", "token": f"HP-{uuid.uuid4()}"}
                elif user == "vibhor" and pwd == "vibhor":
                    resp = {"authenticated": True, "user": "vibhor", "name": "Vibhor (Service Engineer)", "role": "SERVICE_ENGINEER", "token": f"HP-{uuid.uuid4()}"}
                else:
                    resp = {"authenticated": False, "message": "Invalid username or password"}
                self.end_cors(200)
                self.wfile.write(json.dumps(resp).encode("utf-8"))
                return
            except Exception as e:
                self.end_cors(500)
                self.wfile.write(str(e).encode("utf-8"))
                return

        return super().do_POST()

if __name__ == "__main__":
    local_ip = get_local_ip()
    print("=" * 68)
    print("  HP CARE AUTHORISED SERVICE DESK — CENTRAL BACKEND SERVER (v3.0)")
    print("  Partner: HOME COMFORTS · 89 ZONE 2 M.P. NAGAR BHOPAL")
    print("=" * 68)
    print()
    print(f"  [+] Central Server is LIVE and RUNNING!")
    print()
    print(f"  [1] On THIS Machine, open:")
    print(f"      👉 http://localhost:{PORT}")
    print()
    print(f"  [2] On ANY OTHER LAPTOP or MOBILE on same Wi-Fi / LAN, open:")
    print(f"      👉 http://{local_ip}:{PORT}")
    print()
    print("=" * 68)

    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("", PORT), HPCareHTTPRequestHandler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nServer shutting down. Goodbye!")
