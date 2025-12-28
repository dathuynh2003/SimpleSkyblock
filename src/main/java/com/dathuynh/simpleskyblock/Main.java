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

    @Override
    public void onEnable() {
        getLogger().info("SimpleSkyblock plugin đã bật!");

        // Load configs TRƯỚC
        configLoader = new ConfigLoader(this);

        // Managers
        spawnManager = new SpawnManager(this);
        npcManager = new NPCManager(this, configLoader);

        // Commands
        IslandCommand islandCommand = new IslandCommand(this);
        getCommand("is").setExecutor(islandCommand);
        getCommand("spawn").setExecutor(new SpawnCommand(spawnManager));
        getCommand("npc").setExecutor(new NPCCommand(npcManager));
        getCommand("restart").setExecutor(new RestartCommand(this));

        // Event Listeners
        getServer().getPluginManager().registerEvents(new SpawnProtection(), this);
        getServer().getPluginManager().registerEvents(new IslandProtection(islandCommand), this);
        getServer().getPluginManager().registerEvents(npcManager, this);
        getServer().getPluginManager().registerEvents(new TradeMenuListener(configLoader), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("SimpleSkyblock plugin đã tắt!");
    }

    public ConfigLoader getConfigLoader() {
        return configLoader;
    }
}
