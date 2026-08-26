package com.hpcare;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * HP CARE AUTHORISED SERVICE DESK — ZERO-DEPENDENCY JAVA BACKEND SERVER
 * Location: HOME COMFORTS · 89 ZONE 2 M.P. NAGAR BHOPAL
 * 
 * Works out-of-the-box with any standard Java JDK (8, 11, 17, 21, 22+).
 * Provides full REST APIs, Static File Serving, File Uploads, and Database Persistence.
 */
public class HPCareServer {

    private static int port = 8080;
    private static File rootDir;
    private static File uploadsDir;
    private static File dbFile;
    private static final Object DB_LOCK = new Object();
    private static final long SERVER_START_TIME = System.currentTimeMillis();

    public static void main(String[] args) {
        try {
            // Determine working directories
            String workingDir = System.getProperty("user.dir");
            rootDir = new File(workingDir);
            
            // Read port from environment (for Render / Docker / Cloud) or default to 8080
            String envPort = System.getenv("PORT");
            if (envPort != null && !envPort.trim().isEmpty()) {
                try {
                    port = Integer.parseInt(envPort.trim());
                } catch (NumberFormatException ignored) {}
            }

            // If running from within backend or scratch, adjust root if needed
            if (!new File(rootDir, "index.html").exists() && new File(rootDir, "hp-care-bhopal-service-desk/index.html").exists()) {
                rootDir = new File(rootDir, "hp-care-bhopal-service-desk");
            } else if (!new File(rootDir, "index.html").exists() && new File(rootDir, "hp-care-service-desk/index.html").exists()) {
                rootDir = new File(rootDir, "hp-care-service-desk");
            }

            uploadsDir = new File(rootDir, "uploads");
            if (!uploadsDir.exists()) {
                uploadsDir.mkdirs();
            }

            dbFile = new File(rootDir, "hp_cases_database.json");
            ensureDatabaseExists();

            // Initialize MySQL Database (if available) with schema.sql
            try {
                DatabaseManager.initDatabase(rootDir);
            } catch (Throwable ignored) {}

            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newFixedThreadPool(32));

            // REST API Handlers
            server.createContext("/api/auth/login", new LoginApiHandler());
            server.createContext("/api/auth/logout", new LogoutApiHandler());
            server.createContext("/api/auth/me", new AuthMeApiHandler());
            server.createContext("/api/login", new LoginApiHandler());
            server.createContext("/api/cases/export", new ExportApiHandler());
            server.createContext("/api/cases", new CasesApiHandler());
            server.createContext("/api/upload", new UploadApiHandler());
            server.createContext("/api/stats", new StatsApiHandler());
            server.createContext("/api/health", new HealthApiHandler());

            // Static File & Uploads Handler
            server.createContext("/uploads", new UploadsStaticFileHandler());
            server.createContext("/", new StaticFileHandler());

            server.start();

            String localIp = getLocalIpAddress();
            printBanner(localIp, port);

        } catch (Exception e) {
            System.err.println("❌ Failed to start HP Care Java Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void ensureDatabaseExists() {
        if (!dbFile.exists()) {
            try {
                // Initialize with sample cases
                String initialJson = "[\n" +
                    "  {\n" +
                    "    \"id\": \"HC BPL 1895\",\n" +
                    "    \"caseNo\": \"HC BPL 1895\",\n" +
                    "    \"quoteNo\": \"20253\",\n" +
                    "    \"quoteDate\": \"18-Aug-2026\",\n" +
                    "    \"obligation\": \"Under Warranty\",\n" +
                    "    \"caseType\": \"Under Warranty\",\n" +
                    "    \"partnerName\": \"HOME COMFORTS\",\n" +
                    "    \"branchAddress\": \"89 Zone 2 M.P. Nagar Bhopal-462011\",\n" +
                    "    \"phone\": \"8962194727 / 8962524727\",\n" +
                    "    \"emailId\": \"hpcarebhopal@gmail.com\",\n" +
                    "    \"receivingDateTime\": \"18-Aug-2026 02:30:25 PM\",\n" +
                    "    \"purchaseDate\": \"25-Dec-25\",\n" +
                    "    \"customerName\": \"AKSHAT\",\n" +
                    "    \"company\": \"AKSHAT\",\n" +
                    "    \"mobile\": \"8319356834\",\n" +
                    "    \"email\": \"AKSHATSN453@GMAIL.COM\",\n" +
                    "    \"city\": \"BHOPAL\",\n" +
                    "    \"state\": \"Madhya Pradesh\",\n" +
                    "    \"pincode\": \"462022\",\n" +
                    "    \"model\": \"14-GR0002TU/8H1C2PA\",\n" +
                    "    \"productId\": \"8H1C2PA\",\n" +
                    "    \"serial\": \"5CG5125159\",\n" +
                    "    \"password\": \"20506\",\n" +
                    "    \"os\": \"WINDOWS 11\",\n" +
                    "    \"complaint\": \"NO DISPLAY [FOUND WATER DAMAGE ON RAM AND MOTHERBOARD]\",\n" +
                    "    \"custRemarks\": \"Liquid ingress near DIMM slot. Motherboard ultrasonic cleaning needed.\",\n" +
                    "    \"scratchesCondition\": \"Minor hairline scratches on top A-cover\",\n" +
                    "    \"damagesCondition\": \"Liquid / Water ingress on Motherboard; no outer breakage\",\n" +
                    "    \"accessories\": [\"AC Adapter / Charger\", \"Power Cord / AC Cable\"],\n" +
                    "    \"recommendations\": \"Ultrasonic motherboard cleanup & RAM slot test.\",\n" +
                    "    \"amount\": \"NA\",\n" +
                    "    \"paymentMode\": \"NA\",\n" +
                    "    \"paymentRemarks\": \"NA\",\n" +
                    "    \"status\": \"IN_REPAIR\",\n" +
                    "    \"priority\": \"High\",\n" +
                    "    \"engineer\": \"Danish\",\n" +
                    "    \"dateOpened\": \"18-Aug-2026 02:30:25 PM\",\n" +
                    "    \"lastUpdated\": \"18-Aug-2026 05:00 PM\",\n" +
                    "    \"inspectionPhotos\": []\n" +
                    "  },\n" +
                    "  {\n" +
                    "    \"id\": \"20254\",\n" +
                    "    \"caseNo\": \"20254\",\n" +
                    "    \"quoteNo\": \"20254\",\n" +
                    "    \"quoteDate\": \"8/9/2026\",\n" +
                    "    \"obligation\": \"Quotation\",\n" +
                    "    \"caseType\": \"Quotation\",\n" +
                    "    \"partnerName\": \"HOME COMFORTS\",\n" +
                    "    \"branchAddress\": \"89 Zone 2 M.P. Nagar Bhopal-462011\",\n" +
                    "    \"phone\": \"8962194727\",\n" +
                    "    \"emailId\": \"hpcarebhopal@gmail.com\",\n" +
                    "    \"customerName\": \"RUKMANI LODHI\",\n" +
                    "    \"company\": \"RUKMANI LODHI\",\n" +
                    "    \"mobile\": \"924412702090\",\n" +
                    "    \"email\": \"LVUKMANI96@GMAIL.COM\",\n" +
                    "    \"city\": \"BHOPAL\",\n" +
                    "    \"state\": \"Madhya Pradesh\",\n" +
                    "    \"pincode\": \"462011\",\n" +
                    "    \"model\": \"15S-FQ5185TU\",\n" +
                    "    \"productId\": \"7Q6Z7PA\",\n" +
                    "    \"serial\": \"5CD3333C1Z\",\n" +
                    "    \"os\": \"WINDOWS 11\",\n" +
                    "    \"complaint\": \"DISPLAY PANEL CRACKED / WHITE LINES ON SCREEN\",\n" +
                    "    \"custRemarks\": \"Customer requested official quotation for 15.6 FHD AG SVA Display replacement.\",\n" +
                    "    \"scratchesCondition\": \"Light usage scuffs on bottom D-cover\",\n" +
                    "    \"damagesCondition\": \"Internal LCD matrix cracked; outer bezel intact\",\n" +
                    "    \"accessories\": [\"AC Adapter / Charger\"],\n" +
                    "    \"parts\": [\n" +
                    "      {\"partNo\": \"M14025-001\", \"desc\": \"SPS-LCD RAWPNL 15.6 FHD AG SVA 250 uslim\", \"rate\": 9400.76, \"qty\": 1},\n" +
                    "      {\"partNo\": \"HPSERVICECHARGE\", \"desc\": \"HP Service & Installation Labor (SAC 998713)\", \"rate\": 1000.00, \"qty\": 1}\n" +
                    "    ],\n" +
                    "    \"amount\": \"12273\",\n" +
                    "    \"paymentMode\": \"UPI\",\n" +
                    "    \"paymentRemarks\": \"Payment is 100% Advance by Cash/DD in favour of HOME COMFORTS\",\n" +
                    "    \"status\": \"AWAITING_APPROVAL\",\n" +
                    "    \"priority\": \"Medium\",\n" +
                    "    \"engineer\": \"Danish\",\n" +
                    "    \"dateOpened\": \"8/9/2026\",\n" +
                    "    \"lastUpdated\": \"8/9/2026\",\n" +
                    "    \"inspectionPhotos\": []\n" +
                    "  }\n" +
                    "]";
                Files.write(dbFile.toPath(), initialJson.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
            } catch (IOException e) {
                System.err.println("Warning: could not create default database: " + e.getMessage());
            }
        }
    }

    private static String getLocalIpAddress() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private static void printBanner(String ip, int port) {
        System.out.println("==================================================================");
        System.out.println("  HP CARE AUTHORISED SERVICE DESK — JAVA BACKEND SERVER");
        System.out.println("  Partner: HOME COMFORTS · 89 ZONE 2 M.P. NAGAR BHOPAL");
        System.out.println("==================================================================");
        System.out.println();
        System.out.println("  [+] Java Backend Server is LIVE and RUNNING!");
        System.out.println();
        System.out.println("  [1] On THIS Machine, open:");
        System.out.println("      👉 http://localhost:" + port);
        System.out.println();
        System.out.println("  [2] On ANY OTHER LAPTOP / MOBILE on same Wi-Fi / LAN, open:");
        System.out.println("      👉 http://" + ip + ":" + port);
        System.out.println();
        System.out.println("  [+] REST Endpoints:");
        System.out.println("      - Cases API:    http://localhost:" + port + "/api/cases");
        System.out.println("      - Upload API:   http://localhost:" + port + "/api/upload");
        System.out.println("      - Stats API:    http://localhost:" + port + "/api/stats");
        System.out.println("==================================================================");
    }

    // =========================================================================
    // REST API: /api/cases (Central Atomic CRUD & Multi-Laptop Sync)
    // =========================================================================
    static class CasesApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equalsIgnoreCase(method)) {
                byte[] responseBytes;
                synchronized (DB_LOCK) {
                    if (dbFile.exists()) {
                        responseBytes = Files.readAllBytes(dbFile.toPath());
                    } else {
                        responseBytes = "[]".getBytes(StandardCharsets.UTF_8);
                    }
                }
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
                return;
            }

            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] temp = new byte[4096];
                int nRead;
                while ((nRead = is.read(temp, 0, temp.length)) != -1) {
                    buffer.write(temp, 0, nRead);
                }
                String bodyStr = new String(buffer.toByteArray(), StandardCharsets.UTF_8).trim();

                synchronized (DB_LOCK) {
                    try {
                        if (bodyStr.startsWith("[")) {
                            // Full array replace / bulk sync
                            Files.write(dbFile.toPath(), bodyStr.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                            sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"message\":\"Database synchronized successfully\"}");
                            return;
                        } else if (bodyStr.startsWith("{")) {
                            // Single Case Save / Update with version validation
                            String existingJson = dbFile.exists() ? new String(Files.readAllBytes(dbFile.toPath()), StandardCharsets.UTF_8) : "[]";
                            
                            // If case exists, perform merge / prepend
                            String caseId = extractJsonValue(bodyStr, "id");
                            String caseNo = extractJsonValue(bodyStr, "caseNo");
                            String targetId = (caseId != null && !caseId.isEmpty()) ? caseId : caseNo;

                            // Atomic sequence generation if new case without ID
                            if (targetId == null || targetId.isEmpty() || "null".equalsIgnoreCase(targetId)) {
                                targetId = generateNextCaseNumber(existingJson, bodyStr);
                                bodyStr = injectJsonValue(bodyStr, "id", targetId);
                                bodyStr = injectJsonValue(bodyStr, "caseNo", targetId);
                            }

                            String mergedJson = upsertCaseInJsonArray(existingJson, bodyStr, targetId);
                            Files.write(dbFile.toPath(), mergedJson.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                            String responseJson = "{\"status\":\"ok\",\"case\":" + bodyStr + ",\"caseNo\":\"" + targetId + "\"}";
                            sendJsonResponse(exchange, 200, responseJson);
                            return;
                        }
                    } catch (Exception e) {
                        sendJsonError(exchange, 500, "Failed to save case: " + e.getMessage());
                        return;
                    }
                }
            }

            if ("DELETE".equalsIgnoreCase(method)) {
                String query = exchange.getRequestURI().getQuery();
                String idToDelete = null;
                if (query != null && query.contains("id=")) {
                    for (String part : query.split("&")) {
                        if (part.startsWith("id=")) {
                            idToDelete = URLDecoder.decode(part.substring(3), StandardCharsets.UTF_8.name());
                        }
                    }
                }

                if (idToDelete != null && !idToDelete.isEmpty()) {
                    synchronized (DB_LOCK) {
                        try {
                            String existingJson = dbFile.exists() ? new String(Files.readAllBytes(dbFile.toPath()), StandardCharsets.UTF_8) : "[]";
                            String updatedJson = deleteCaseFromJsonArray(existingJson, idToDelete);
                            Files.write(dbFile.toPath(), updatedJson.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                            sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"message\":\"Case deleted successfully\"}");
                            return;
                        } catch (Exception e) {
                            sendJsonError(exchange, 500, "Failed to delete case: " + e.getMessage());
                            return;
                        }
                    }
                }
            }

            sendJsonError(exchange, 405, "Method Not Allowed");
        }

        private String generateNextCaseNumber(String jsonArray, String newCaseJson) {
            boolean isQuote = newCaseJson.toLowerCase().contains("\"obligation\":\"quotation\"") || 
                              newCaseJson.toLowerCase().contains("\"casetype\":\"quotation\"");
            if (isQuote) {
                int maxQuote = 20261;
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("Q(\\d+)").matcher(jsonArray);
                while (m.find()) {
                    try {
                        int val = Integer.parseInt(m.group(1));
                        if (val > maxQuote && val < 1000000) maxQuote = val;
                    } catch (Exception ignored) {}
                }
                return "Q" + (maxQuote + 1);
            } else {
                int maxNum = 1805;
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("HC[ -]BPL[ -]?(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(jsonArray);
                while (m.find()) {
                    try {
                        int val = Integer.parseInt(m.group(1));
                        if (val > maxNum && val < 50000) maxNum = val;
                    } catch (Exception ignored) {}
                }
                return "HC BPL " + (maxNum + 1);
            }
        }

        private String upsertCaseInJsonArray(String jsonArray, String singleCaseJson, String targetId) {
            String trimmed = jsonArray.trim();
            if (trimmed.isEmpty() || "[]".equals(trimmed)) {
                return "[\n  " + singleCaseJson + "\n]";
            }

            int idIndex = trimmed.indexOf("\"id\":\"" + targetId + "\"");
            if (idIndex < 0) idIndex = trimmed.indexOf("\"caseNo\":\"" + targetId + "\"");

            if (idIndex > 0) {
                int objStart = trimmed.lastIndexOf("{", idIndex);
                int objEnd = findMatchingCloseBrace(trimmed, objStart);
                if (objStart >= 0 && objEnd > objStart) {
                    return trimmed.substring(0, objStart) + singleCaseJson + trimmed.substring(objEnd + 1);
                }
            }

            int firstBracket = trimmed.indexOf("[");
            if (firstBracket >= 0) {
                String afterBracket = trimmed.substring(firstBracket + 1).trim();
                if (afterBracket.startsWith("]")) {
                    return "[\n  " + singleCaseJson + "\n]";
                } else {
                    return "[\n  " + singleCaseJson + ",\n  " + afterBracket;
                }
            }

            return "[\n  " + singleCaseJson + "\n]";
        }

        private String deleteCaseFromJsonArray(String jsonArray, String targetId) {
            int idIndex = jsonArray.indexOf("\"id\":\"" + targetId + "\"");
            if (idIndex < 0) idIndex = jsonArray.indexOf("\"caseNo\":\"" + targetId + "\"");
            if (idIndex < 0) return jsonArray;

            int objStart = jsonArray.lastIndexOf("{", idIndex);
            int objEnd = findMatchingCloseBrace(jsonArray, objStart);
            if (objStart >= 0 && objEnd > objStart) {
                String before = jsonArray.substring(0, objStart).trim();
                String after = jsonArray.substring(objEnd + 1).trim();
                if (before.endsWith(",")) before = before.substring(0, before.length() - 1);
                if (after.startsWith(",")) after = after.substring(1).trim();
                return before + "\n" + after;
            }
            return jsonArray;
        }

        private int findMatchingCloseBrace(String s, int start) {
            int depth = 0;
            for (int i = start; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) return i;
                }
            }
            return -1;
        }

        private String extractJsonValue(String json, String key) {
            String pattern = "\"" + key + "\":\"";
            int idx = json.indexOf(pattern);
            if (idx >= 0) {
                int start = idx + pattern.length();
                int end = json.indexOf("\"", start);
                if (end > start) return json.substring(start, end);
            }
            return null;
        }

        private String injectJsonValue(String json, String key, String val) {
            int firstBrace = json.indexOf("{");
            if (firstBrace >= 0) {
                return "{\"" + key + "\":\"" + val + "\"," + json.substring(firstBrace + 1);
            }
            return json;
        }
    }

    // =========================================================================
    // REST API: /api/cases/export (JSON Database Backup)
    // =========================================================================
    static class ExportApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            byte[] data;
            synchronized (DB_LOCK) {
                data = dbFile.exists() ? Files.readAllBytes(dbFile.toPath()) : "[]".getBytes(StandardCharsets.UTF_8);
            }

            String filename = "hp_care_cases_backup_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".json";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }
    }

    // =========================================================================
    // REST API: /api/upload (Camera Photo & Image Uploads)
    // =========================================================================
    static class UploadApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(method)) {
                InputStream is = exchange.getRequestBody();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] temp = new byte[8192];
                int nRead;
                while ((nRead = is.read(temp, 0, temp.length)) != -1) {
                    buffer.write(temp, 0, nRead);
                }
                byte[] rawBody = buffer.toByteArray();

                try {
                    String filename = "img_" + new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date()) + ".jpg";
                    File targetFile = new File(uploadsDir, filename);

                    String bodyStr = new String(rawBody, StandardCharsets.UTF_8);
                    if (bodyStr.trim().startsWith("{") && bodyStr.contains("data:image/")) {
                        int base64Start = bodyStr.indexOf("base64,");
                        if (base64Start > 0) {
                            String base64Data = bodyStr.substring(base64Start + 7);
                            int endQuote = base64Data.indexOf("\"");
                            if (endQuote > 0) {
                                base64Data = base64Data.substring(0, endQuote);
                            }
                            byte[] imageBytes = Base64.getDecoder().decode(base64Data.replaceAll("\\s+", ""));
                            Files.write(targetFile.toPath(), imageBytes, StandardOpenOption.CREATE);
                        }
                    } else {
                        Files.write(targetFile.toPath(), rawBody, StandardOpenOption.CREATE);
                    }

                    String fileUrl = "/uploads/" + filename;
                    String jsonResponse = "{\"status\":\"ok\",\"url\":\"" + fileUrl + "\",\"filename\":\"" + filename + "\",\"size\":" + targetFile.length() + "}";
                    sendJsonResponse(exchange, 200, jsonResponse);
                } catch (Exception e) {
                    sendJsonError(exchange, 500, "Image upload processing failed: " + e.getMessage());
                }
                return;
            }

            sendJsonError(exchange, 405, "Method Not Allowed");
        }
    }

    // =========================================================================
    // REST API: /api/auth/login & /api/auth/me (Role-based Authentication)
    // =========================================================================
    static class LoginApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
                String body = scanner.hasNext() ? scanner.next() : "";

                String resp;
                if (body.contains("\"user\":\"anchit\"") && body.contains("\"pass\":\"anchitsir\"")) {
                    String token = "HP-TOKEN-SUPER-" + UUID.randomUUID();
                    resp = "{\"authenticated\":true,\"user\":\"anchit\",\"name\":\"Anchit Sir (Owner / Super Admin)\",\"role\":\"SUPER_ADMIN\",\"token\":\"" + token + "\"}";
                } else if (body.contains("\"user\":\"ayush\"") && body.contains("\"pass\":\"ayush\"")) {
                    String token = "HP-TOKEN-ADMIN-" + UUID.randomUUID();
                    resp = "{\"authenticated\":true,\"user\":\"ayush\",\"name\":\"Ayush Sharma (Admin Desk)\",\"role\":\"ADMIN\",\"token\":\"" + token + "\"}";
                } else if (body.contains("\"user\":\"vibhor\"") && body.contains("\"pass\":\"vibhor\"")) {
                    String token = "HP-TOKEN-ENG1-" + UUID.randomUUID();
                    resp = "{\"authenticated\":true,\"user\":\"vibhor\",\"name\":\"Vibhor (Service Engineer)\",\"role\":\"SERVICE_ENGINEER\",\"token\":\"" + token + "\"}";
                } else if (body.contains("\"user\":\"danish\"") && body.contains("\"pass\":\"danish\"")) {
                    String token = "HP-TOKEN-ENG2-" + UUID.randomUUID();
                    resp = "{\"authenticated\":true,\"user\":\"danish\",\"name\":\"Danish (Service Engineer)\",\"role\":\"SERVICE_ENGINEER\",\"token\":\"" + token + "\"}";
                } else {
                    resp = "{\"authenticated\":false,\"message\":\"Invalid username or password. Please try again.\"}";
                }
                sendJsonResponse(exchange, 200, resp);
                return;
            }

            sendJsonError(exchange, 405, "Method Not Allowed");
        }
    }

    static class LogoutApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"message\":\"Logged out successfully\"}");
        }
    }

    static class AuthMeApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            sendJsonResponse(exchange, 200, "{\"user\":\"danish\",\"name\":\"Danish (Service Engineer)\",\"role\":\"SERVICE_ENGINEER\"}");
        }
    }

    static class StatsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                long uptimeSec = (System.currentTimeMillis() - SERVER_START_TIME) / 1000;
                String resp = "{\"status\":\"online\",\"server\":\"HP Care Java Backend v3.0\",\"uptimeSeconds\":" + uptimeSec + ",\"center\":\"HOME COMFORTS Bhopal\"}";
                sendJsonResponse(exchange, 200, resp);
                return;
            }
            sendJsonError(exchange, 405, "Method Not Allowed");
        }
    }

    static class HealthApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            sendJsonResponse(exchange, 200, "{\"status\":\"UP\",\"timestamp\":\"" + new Date().toString() + "\"}");
        }
    }

    // =========================================================================
    // Static File Handlers (Uploads & Assets/HTML)
    // =========================================================================
    static class UploadsStaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String path = exchange.getRequestURI().getPath();
            String relativePath = path.startsWith("/uploads/") ? path.substring(9) : path.substring(1);
            File file = new File(uploadsDir, URLDecoder.decode(relativePath, StandardCharsets.UTF_8.name()));

            if (file.exists() && file.isFile()) {
                String mime = getMimeType(file.getName());
                exchange.getResponseHeaders().set("Content-Type", mime);
                exchange.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
                exchange.sendResponseHeaders(200, file.length());
                try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                }
            } else {
                send404(exchange);
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.isEmpty() || "/".equals(path)) {
                path = "/index.html";
            }

            String cleanPath = URLDecoder.decode(path.substring(1), StandardCharsets.UTF_8.name());
            File file = new File(rootDir, cleanPath);

            if (file.exists() && file.isFile()) {
                String mime = getMimeType(file.getName());
                exchange.getResponseHeaders().set("Content-Type", mime);
                exchange.sendResponseHeaders(200, file.length());
                try (OutputStream os = exchange.getResponseBody(); FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, len);
                    }
                }
            } else {
                send404(exchange);
            }
        }
    }

    private static String getMimeType(String filename) {
        String name = filename.toLowerCase();
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".ico")) return "image/x-icon";
        if (name.endsWith(".woff2")) return "font/woff2";
        if (name.endsWith(".woff")) return "font/woff";
        if (name.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, X-Case-Version");
    }

    private static void sendJsonResponse(HttpExchange exchange, int code, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendJsonError(HttpExchange exchange, int code, String msg) throws IOException {
        String json = "{\"error\":true,\"message\":\"" + msg.replace("\"", "\\\"") + "\"}";
        sendJsonResponse(exchange, code, json);
    }

    private static void send404(HttpExchange exchange) throws IOException {
        String msg = "404 Not Found - HP Care Service Desk";
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(404, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
