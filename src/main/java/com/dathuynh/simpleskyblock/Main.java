package com.dathuynh.simpleskyblock;

import com.dathuynh.simpleskyblock.commands.*;
import com.dathuynh.simpleskyblock.listeners.*;
import com.dathuynh.simpleskyblock.managers.*;
import com.dathuynh.simpleskyblock.utils.ConfigLoader;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private SpawnManager spawnManager;
    private NPCManager npcManager;
    private ConfigLoader configLoader;
    private IslandManager islandManager;
    private int autoSaveTaskId;

    @Override
    public void onEnable() {
        getLogger().info("SimpleSkyblock plugin đã bật!");

        // Load configs TRƯỚC
        configLoader = new ConfigLoader(this);

        // Managers
        spawnManager = new SpawnManager(this);
        npcManager = new NPCManager(this, configLoader);
        islandManager = new IslandManager(this);

        // Commands
        IslandCommand islandCommand = new IslandCommand(this, islandManager);
        getCommand("is").setExecutor(islandCommand);
        getCommand("spawn").setExecutor(new SpawnCommand(spawnManager));
        getCommand("npc").setExecutor(new NPCCommand(npcManager));
        getCommand("restart").setExecutor(new RestartCommand(this));
        getCommand("tp").setExecutor(new TeleportCommand());

        // Event Listeners
        getServer().getPluginManager().registerEvents(new SpawnProtection(), this);
        getServer().getPluginManager().registerEvents(new IslandProtection(islandManager), this);
        getServer().getPluginManager().registerEvents(npcManager, this);
        getServer().getPluginManager().registerEvents(new TradeMenuListener(configLoader), this);
        getServer().getPluginManager().registerEvents(new RespawnListener(islandManager), this);

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
}
