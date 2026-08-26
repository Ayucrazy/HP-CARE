package com.hpcare;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

/**
 * HP CARE BHOPAL — MYSQL & LOCAL DATABASE MANAGER
 * 
 * Provides automated MySQL detection, database initialization, client session logging,
 * and graceful fallback to JSON database if MySQL is not currently running.
 */
public class DatabaseManager {

    private static String dbHost = "localhost";
    private static int dbPort = 3306;
    private static String dbName = "hp_care_db";
    private static String dbUser = "root";
    private static String dbPass = "root";

    private static boolean isMySqlAvailable = false;

    public static void initDatabase(File rootDir) {
        String[] possiblePass = { dbPass, "", "admin", "password", "123456", "root123" };
        
        for (String pass : possiblePass) {
            if (tryConnectAndInit(pass, rootDir)) {
                dbPass = pass;
                isMySqlAvailable = true;
                System.out.println("  [✔] MySQL Database CONNECTED: jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + " (User: " + dbUser + ")");
                return;
            }
        }

        System.out.println("  [i] MySQL Server not active on port 3306. Operating seamlessly in High-Performance Local JSON Database Mode.");
        isMySqlAvailable = false;
    }

    private static boolean tryConnectAndInit(String pass, File rootDir) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            return false;
        }

        String serverUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        try (Connection conn = DriverManager.getConnection(serverUrl, dbUser, pass);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

            File schemaFile = new File(rootDir, "schema.sql");
            if (schemaFile.exists()) {
                String sql = new String(Files.readAllBytes(schemaFile.toPath()), StandardCharsets.UTF_8);
                try (Connection dbConn = DriverManager.getConnection("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", dbUser, pass);
                     Statement dbStmt = dbConn.createStatement()) {
                    for (String query : sql.split(";")) {
                        String trimmed = query.trim();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("--") && !trimmed.toLowerCase().startsWith("create database") && !trimmed.toLowerCase().startsWith("use ")) {
                            try {
                                dbStmt.execute(trimmed);
                            } catch (SQLException ignored) {}
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void logClientConnection(String clientIp, String deviceInfo, String username, String apiKey) {
        if (!isMySqlAvailable) return;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO connected_clients (client_ip, device_info, username, api_key) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, clientIp != null ? clientIp : "127.0.0.1");
            ps.setString(2, deviceInfo != null ? deviceInfo : "Web Browser");
            ps.setString(3, username != null ? username : "Guest");
            ps.setString(4, apiKey != null ? apiKey : "KEY-HP-" + System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    public static boolean isMySqlActive() {
        return isMySqlAvailable;
    }

    public static Connection getConnection() throws SQLException {
        if (!isMySqlAvailable) {
            throw new SQLException("MySQL is not currently connected");
        }
        String dbUrl = "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DriverManager.getConnection(dbUrl, dbUser, dbPass);
    }
}
