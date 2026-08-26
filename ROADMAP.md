# 🚀 HP CARE BHOPAL — Multi-Laptop Server Setup Roadmap

This guide shows you how to use one laptop as your **Central Server** at the HP Care Service Desk (Bhopal) so that all other counter laptops, technician laptops, and mobile devices on the same Wi-Fi / Office Network can access and manage service cases together in real time.

---

## 🗺️ Step-by-Step Server Setup Roadmap

### 📋 Phase 1: Preparation on the "Server Laptop"
1. Copy the entire `hp-care-service-desk` folder onto the laptop you want to use as your central server.
2. Ensure both the **Server Laptop** and all **Client Laptops** are connected to the **same Wi-Fi router or Office LAN cable**.

---

### ⚡ Phase 2: Start the Server (1-Click)
1. Inside the `hp-care-service-desk` folder, double-click:
   👉 `start-server.bat`
2. A black command window will open displaying your server details:
   ```text
   =========================================================================
     HP CARE AUTHORISED SERVICE DESK — LOCAL NETWORK SERVER
     Partner: HOME COMFORTS · 89 ZONE 2 M.P. NAGAR BHOPAL
   =========================================================================

     [+] Server is LIVE and RUNNING!

     [1] On THIS Server Laptop, open:
         http://localhost:8080

     [2] On ANY OTHER LAPTOP or MOBILE on the same Wi-Fi / LAN, open:
         http://192.168.1.50:8080
   =========================================================================
   ```
   *(Note: `192.168.1.50` will be replaced by your actual laptop IP address automatically)*.

---

### 💻 Phase 3: Accessing from Other Laptops / Front Desk / Technicians
1. On any other laptop in the service center (front desk reception, diagnosis bench, quality check counter):
2. Open Chrome / Edge / Firefox browser.
3. Type the Server IP URL in the address bar, for example:
   ```text
   http://192.168.1.50:8080
   ```
4. Log in using your credentials:
   - **Username:** `danish` (or `ayush`)
   - **Password:** `danish` (or `ayush`)
5. You can now create new cases, print official CSO receipts, update status, and close cases simultaneously!

---

### 🛡️ Phase 4: Windows Firewall Permission (If Other Laptops Cannot Connect)
If other laptops show "Connection Timed Out" or "Site Can't Be Reached", allow Port 8080 through Windows Firewall on the Server Laptop:

1. Press `Windows Key + R`, type `control firewall.cpl` and press Enter.
2. Click **"Advanced settings"** on the left menu.
3. Click **"Inbound Rules"** $\rightarrow$ **"New Rule..."** (on the right).
4. Select **Port** $\rightarrow$ Click Next.
5. Select **TCP** and enter **Specific local ports: `8080`** $\rightarrow$ Click Next.
6. Select **"Allow the connection"** $\rightarrow$ Click Next $\rightarrow$ Check all boxes (Domain, Private, Public) $\rightarrow$ Click Next.
7. Name the rule `HP Care Server (Port 8080)` $\rightarrow$ Click **Finish**.

---

### 💾 Phase 5: Backup & Data Safety
- On the Dashboard, you have an **"📥 Export Database Backup"** button.
- Click this button once daily to download a timestamped `.json` file backup of all customer cases to a USB drive or cloud drive.

---

### 🎯 Roadmap Summary at a Glance

```
       [ Wi-Fi Router / Office Network ]
                     │
       ┌─────────────┴─────────────┐
       │                           │
[ SERVER LAPTOP ]           [ CLIENT LAPTOPS ]
  Runs start-server.bat       Front Desk / Engineers
  IP: 192.168.1.50:8080       Open: http://192.168.1.50:8080
```
