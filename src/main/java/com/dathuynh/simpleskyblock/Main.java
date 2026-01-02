package com.dathuynh.simpleskyblock;

import com.dathuynh.simpleskyblock.commands.*;
import com.dathuynh.simpleskyblock.commands.WarpCommand;
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
    private ItemManager itemManager;
    private IslandManager islandManager;
    private MiningZoneManager miningZoneManager;
    private AuthManager authManager;
    private ArenaManager arenaManager;
    private BossManager bossManager;

    private KitCommand kitCommand;
    private int autoSaveTaskId;

    @Override
    public void onEnable() {
        getLogger().info("SimpleSkyblock plugin đã bật!");

        // Load configs TRƯỚC
        configLoader = new ConfigLoader(this);
        itemManager = new ItemManager(this);
        KitConfig kitConfig = new KitConfig(this);

        // Managers
        spawnManager = new SpawnManager(this);
        npcManager = new NPCManager(this, configLoader);
        authManager = new AuthManager(this);
        islandManager = new IslandManager(this);
        miningZoneManager = new MiningZoneManager(this);
        arenaManager = new ArenaManager(this);
        bossManager = new BossManager(this, arenaManager, itemManager);

        // Commands
        IslandCommand islandCommand = new IslandCommand(this, islandManager);
        kitCommand = new KitCommand(this, islandManager, kitConfig);

        getCommand("is").setExecutor(islandCommand);
        getCommand("spawn").setExecutor(new SpawnCommand(spawnManager));
        getCommand("npc").setExecutor(new NPCCommand(npcManager));
        getCommand("warp").setExecutor(new WarpCommand(miningZoneManager, spawnManager, arenaManager));
        getCommand("init").setExecutor(new InitCommand(spawnManager, miningZoneManager, arenaManager));
        getCommand("restart").setExecutor(new RestartCommand(this));
        getCommand("tp").setExecutor(new TeleportCommand());
        getCommand("kit").setExecutor(kitCommand);
        getCommand("boss").setExecutor(new BossCommand(bossManager));

        // Auth commands
        getCommand("register").setExecutor(new AuthCommand(authManager, "register"));
        getCommand("login").setExecutor(new AuthCommand(authManager, "login"));
        getCommand("changepassword").setExecutor(new AuthCommand(authManager, "changepassword"));

        // Event Listeners
        getServer().getPluginManager().registerEvents(new AuthListener(authManager, this), this);
        getServer().getPluginManager().registerEvents(new SpawnProtection(), this);
        getServer().getPluginManager().registerEvents(new JoinListener(spawnManager, this), this);
        getServer().getPluginManager().registerEvents(new IslandProtection(islandManager), this);
        getServer().getPluginManager().registerEvents(npcManager, this);
        getServer().getPluginManager().registerEvents(new TradeMenuListener(configLoader), this);
        getServer().getPluginManager().registerEvents(new RespawnListener(islandManager, spawnManager), this);
        getServer().getPluginManager().registerEvents(new IslandPvPProtection(islandManager), this);
        getServer().getPluginManager().registerEvents(new MiningZoneProtection(miningZoneManager), this);
        getServer().getPluginManager().registerEvents(new ArenaProtection(arenaManager), this);
        getServer().getPluginManager().registerEvents(new BossListener(bossManager, arenaManager), this);
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

        if (authManager != null) {
            authManager.saveAuthData();
            getLogger().info("Đã lưu auth data!");
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

        if (miningZoneManager != null) {
            miningZoneManager.stopAutoReset();
        }

        if (bossManager != null) {
            bossManager.shutdown();
        }

        getLogger().info("SimpleSkyblock plugin đã tắt!");
    }

    public ConfigLoader getConfigLoader() {
        return configLoader;
    }

    public ItemManager getItemManager() {
        return itemManager;
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

    public SpawnManager getSpawnManager() {
        return spawnManager;
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

    public BossManager getBossManager() {
        return bossManager;
    }

}
