package com.dathuynh.simpleskyblock;

import com.dathuynh.simpleskyblock.commands.*;
import com.dathuynh.simpleskyblock.listeners.*;
import com.dathuynh.simpleskyblock.managers.*;
import com.dathuynh.simpleskyblock.utils.ConfigLoader;
import com.dathuynh.simpleskyblock.utils.KitConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class Main extends JavaPlugin {

    private SpawnManager spawnManager;
    private NPCManager npcManager;
    private ConfigLoader configLoader;
    private IslandManager islandManager;
    private KitCommand kitCommand;
    private int autoSaveTaskId;

    @Override
    public void onEnable() {
        getLogger().info("SimpleSkyblock plugin đã bật!");

        // Load configs TRƯỚC
        configLoader = new ConfigLoader(this);
        KitConfig kitConfig = new KitConfig(this);

        // Managers
        spawnManager = new SpawnManager(this);
        npcManager = new NPCManager(this, configLoader);
        islandManager = new IslandManager(this);

        // Commands
        IslandCommand islandCommand = new IslandCommand(this, islandManager);
        kitCommand = new KitCommand(this, islandManager, kitConfig);

        getCommand("is").setExecutor(islandCommand);
        getCommand("spawn").setExecutor(new SpawnCommand(spawnManager));
        getCommand("npc").setExecutor(new NPCCommand(npcManager));
        getCommand("restart").setExecutor(new RestartCommand(this));
        getCommand("tp").setExecutor(new TeleportCommand());
        getCommand("kit").setExecutor(kitCommand);

        // Event Listeners
        getServer().getPluginManager().registerEvents(new SpawnProtection(), this);
        getServer().getPluginManager().registerEvents(new IslandProtection(islandManager), this);
        getServer().getPluginManager().registerEvents(npcManager, this);
        getServer().getPluginManager().registerEvents(new TradeMenuListener(configLoader), this);
        getServer().getPluginManager().registerEvents(new RespawnListener(islandManager), this);
        getServer().getPluginManager().registerEvents(new IslandPvPProtection(islandManager), this);

        // Load kit data
        loadKitData();

        // Auto-save mỗi 5 phút (6000 ticks = 300 giây)
        startAutoSave();
    }

    @Override
    public void onDisable() {
        // Hủy auto-save task
        if (autoSaveTaskId != -1) {
            getServer().getScheduler().cancelTask(autoSaveTaskId);
        }
        // Save data cuối cùng trước khi tắt
        if (islandManager != null) {
            islandManager.saveData();
            getLogger().info("Đã lưu island data!");
        }

        if (kitCommand != null) {
            saveKitData();
            getLogger().info("Đã lưu kit data!");
        }

        getLogger().info("SimpleSkyblock plugin đã tắt!");
    }

    public ConfigLoader getConfigLoader() {
        return configLoader;
    }

    private void startAutoSave() {
        // Auto-save mỗi 5 phút = 6000 ticks (20 ticks = 1 giây)
        // Delay 6000 ticks để save lần đầu sau 5 phút
        autoSaveTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                if (islandManager != null) {
                    islandManager.saveData();
                    getLogger().info("Auto-saved island data!");
                }
            }
        }, 6000L, 6000L); // delay 6000 ticks, repeat mỗi 6000 ticks
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }

    private void loadKitData() {
        File file = new File(getDataFolder(), "kits.yml");
        if (!file.exists()) return;

        org.bukkit.configuration.file.YamlConfiguration config =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);

        // Load once-used kits
        if (config.contains("used-once")) {
            Map<UUID, Set<String>> usedOnce = new HashMap<>();

            for (String uuidStr : config.getConfigurationSection("used-once").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<String> kitIds = config.getStringList("used-once." + uuidStr);
                    usedOnce.put(uuid, new HashSet<>(kitIds));
                } catch (Exception e) {
                    getLogger().warning("Lỗi load used-once kit: " + uuidStr);
                }
            }

            kitCommand.setUsedOnceKits(usedOnce);
        }

        // Load cooldown kits
        if (config.contains("cooldowns")) {
            Map<String, Map<UUID, Long>> cooldowns = new HashMap<>();

            for (String kitId : config.getConfigurationSection("cooldowns").getKeys(false)) {
                Map<UUID, Long> playerCooldowns = new HashMap<>();

                org.bukkit.configuration.ConfigurationSection kitSection =
                        config.getConfigurationSection("cooldowns." + kitId);

                for (String uuidStr : kitSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        long timestamp = config.getLong("cooldowns." + kitId + "." + uuidStr);
                        playerCooldowns.put(uuid, timestamp);
                    } catch (Exception e) {
                        getLogger().warning("Lỗi load cooldown: " + kitId + " - " + uuidStr);
                    }
                }

                cooldowns.put(kitId, playerCooldowns);
            }

            kitCommand.setCooldownKits(cooldowns);
        }

        getLogger().info("Đã load kit data!");
    }

    private void saveKitData() {
        File file = new File(getDataFolder(), "kits.yml");
        org.bukkit.configuration.file.YamlConfiguration config =
                new org.bukkit.configuration.file.YamlConfiguration();

        // Save once-used kits
        for (Map.Entry<UUID, Set<String>> entry : kitCommand.getUsedOnceKits().entrySet()) {
            config.set("used-once." + entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }

        // Save cooldown kits
        for (Map.Entry<String, Map<UUID, Long>> entry : kitCommand.getCooldownKits().entrySet()) {
            String kitId = entry.getKey();
            for (Map.Entry<UUID, Long> playerEntry : entry.getValue().entrySet()) {
                config.set("cooldowns." + kitId + "." + playerEntry.getKey().toString(),
                        playerEntry.getValue());
            }
        }

        try {
            config.save(file);
        } catch (java.io.IOException e) {
            getLogger().severe("Lỗi save kit data: " + e.getMessage());
        }
    }

}
