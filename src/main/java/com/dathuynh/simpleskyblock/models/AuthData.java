package com.dathuynh.simpleskyblock.models;

import java.util.HashMap;
import java.util.Map;

public class AuthData {
    private String uuid;
    private String hashedPassword;
    private String lastIP;
    private long lastLogin;

    public AuthData() {}

    public AuthData(String uuid, String hashedPassword, String lastIP, long lastLogin) {
        this.uuid = uuid;
        this.hashedPassword = hashedPassword;
        this.lastIP = lastIP;
        this.lastLogin = lastLogin;
    }

    // Getters and Setters
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getLastIP() {
        return lastIP;
    }

    public void setLastIP(String lastIP) {
        this.lastIP = lastIP;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    // Convert to Map for JSON
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", uuid);
        map.put("hashedPassword", hashedPassword);
        map.put("lastIP", lastIP);
        map.put("lastLogin", lastLogin);
        return map;
    }

    // Create from Map
    public static AuthData fromMap(Map<String, Object> map) {
        return new AuthData(
                (String) map.get("uuid"),
                (String) map.get("hashedPassword"),
                (String) map.get("lastIP"),
                ((Number) map.get("lastLogin")).longValue()
        );
    }
}
