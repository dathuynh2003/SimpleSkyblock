package com.dathuynh.simpleskyblock.managers;

import com.dathuynh.simpleskyblock.Main;
import com.dathuynh.simpleskyblock.models.AuthData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class AuthManager {

    private Main plugin;
    private Map<UUID, AuthData> authDatabase;
    private Set<UUID> loggedInPlayers;
    private Gson gson;
    private File authFile;

    public AuthManager(Main plugin) {
        this.plugin = plugin;
        this.authDatabase = new HashMap<>();
        this.loggedInPlayers = new HashSet<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.authFile = new File(plugin.getDataFolder(), "auth.json");

        loadAuthData();
    }

    /**
     * Đăng ký tài khoản mới
     */
    public boolean register(Player player, String password) {
        UUID uuid = player.getUniqueId();

        // Check đã đăng ký chưa
        if (authDatabase.containsKey(uuid)) {
            return false; // Đã đăng ký rồi
        }

        // Hash password bằng BCrypt
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

        // Lưu vào database
        AuthData authData = new AuthData(
                uuid.toString(),
                hashedPassword,
                getPlayerIP(player),
                System.currentTimeMillis()
        );

        authDatabase.put(uuid, authData);
        saveAuthData();

        return true;
    }

    /**
     * Đăng nhập
     */
    public boolean login(Player player, String password) {
        UUID uuid = player.getUniqueId();

        // Check đã đăng ký chưa
        if (!authDatabase.containsKey(uuid)) {
            return false; // Chưa đăng ký
        }

        AuthData authData = authDatabase.get(uuid);

        // Verify password với BCrypt
        if (BCrypt.checkpw(password, authData.getHashedPassword())) {
            // Update last login info
            authData.setLastIP(getPlayerIP(player));
            authData.setLastLogin(System.currentTimeMillis());
            saveAuthData();

            // Add vào logged in list
            loggedInPlayers.add(uuid);
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            return true;
        }

        return false;
    }

    /**
     * Đổi mật khẩu
     */
    public boolean changePassword(Player player, String oldPassword, String newPassword) {
        UUID uuid = player.getUniqueId();

        if (!authDatabase.containsKey(uuid)) {
            return false;
        }

        AuthData authData = authDatabase.get(uuid);

        // Verify old password
        if (!BCrypt.checkpw(oldPassword, authData.getHashedPassword())) {
            return false; // Mật khẩu cũ sai
        }

        // Hash new password
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        authData.setHashedPassword(hashedPassword);
        saveAuthData();

        return true;
    }

    /**
     * Check player đã đăng ký chưa
     */
    public boolean isRegistered(UUID uuid) {
        return authDatabase.containsKey(uuid);
    }

    /**
     * Check player đã login chưa
     */
    public boolean isLoggedIn(UUID uuid) {
        return loggedInPlayers.contains(uuid);
    }

    /**
     * Auto-login nếu cùng IP (trong vòng 24h)
     */
    public boolean canAutoLogin(Player player) {
        UUID uuid = player.getUniqueId();

        if (!authDatabase.containsKey(uuid)) {
            return false;
        }

        AuthData authData = authDatabase.get(uuid);
        String currentIP = getPlayerIP(player);
        long timeSinceLastLogin = System.currentTimeMillis() - authData.getLastLogin();

        // Auto-login nếu cùng IP và login trong 24h
        return currentIP.equals(authData.getLastIP()) && timeSinceLastLogin < 86400000L;
    }

    /**
     * Logout khi disconnect
     */
    public void logout(UUID uuid) {
        loggedInPlayers.remove(uuid);
    }

    /**
     * Get player IP
     */
    private String getPlayerIP(Player player) {
        return player.getAddress().getAddress().getHostAddress();
    }

    /**
     * Load auth data từ file
     */
    private void loadAuthData() {
        if (!authFile.exists()) {
            plugin.getLogger().info("Auth file chưa tồn tại, tạo mới...");
            return;
        }

        try (FileReader reader = new FileReader(authFile)) {
            Type type = new TypeToken<Map<String, Map<String, Object>>>() {
            }.getType();
            Map<String, Map<String, Object>> rawData = gson.fromJson(reader, type);

            if (rawData != null) {
                for (Map.Entry<String, Map<String, Object>> entry : rawData.entrySet()) {
                    UUID uuid = UUID.fromString(entry.getKey());
                    AuthData authData = AuthData.fromMap(entry.getValue());
                    authDatabase.put(uuid, authData);
                }
            }

            plugin.getLogger().info("✓ Đã load " + authDatabase.size() + " tài khoản!");
        } catch (IOException e) {
            plugin.getLogger().severe("Lỗi load auth data: " + e.getMessage());
        }
    }

    /**
     * Save auth data vào file
     */
    public void saveAuthData() {
        try (FileWriter writer = new FileWriter(authFile)) {
            Map<String, Map<String, Object>> rawData = new HashMap<>();

            for (Map.Entry<UUID, AuthData> entry : authDatabase.entrySet()) {
                rawData.put(entry.getKey().toString(), entry.getValue().toMap());
            }

            gson.toJson(rawData, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Lỗi save auth data: " + e.getMessage());
        }
    }
}
