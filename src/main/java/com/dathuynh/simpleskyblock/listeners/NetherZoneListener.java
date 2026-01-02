package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.Main;
import com.dathuynh.simpleskyblock.managers.NetherZoneManager;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class NetherZoneListener implements Listener {

    private Main plugin;
    private NetherZoneManager netherZoneManager;
    private Random random;

    // Drop rate settings (loaded from config)
    private double boneRate;
    private double coalRate;
    private double skullRate;
    private double swordRate;
    private boolean reduceXp;
    private double xpMultiplier;
    private boolean playerKillOnly;

    // Enable detailed logging?
    private boolean detailedLogging;

    public NetherZoneListener(Main plugin, NetherZoneManager netherZoneManager) {
        this.plugin = plugin;
        this.netherZoneManager = netherZoneManager;
        this.random = new Random();

        // Load settings from config
        loadConfig();
    }

    /**
     * Load drop rate settings from config.yml
     */
    public void loadConfig() {
        this.boneRate = plugin.getConfig().getDouble("nether_zone.wither_skeleton_drops.bone_rate", 0.3);
        this.coalRate = plugin.getConfig().getDouble("nether_zone.wither_skeleton_drops.coal_rate", 0.5);
        this.skullRate = plugin.getConfig().getDouble("nether_zone.wither_skeleton_drops.skull_rate", 0.1);
        this.swordRate = plugin.getConfig().getDouble("nether_zone.wither_skeleton_drops.stone_sword_rate", 1.0);
        this.reduceXp = plugin.getConfig().getBoolean("nether_zone.wither_skeleton_drops.reduce_xp", true);
        this.xpMultiplier = plugin.getConfig().getDouble("nether_zone.wither_skeleton_drops.xp_multiplier", 0.5);
        this.playerKillOnly = plugin.getConfig().getBoolean("nether_zone.wither_skeleton_drops.player_kill_only", true);
        this.detailedLogging = plugin.getConfig().getBoolean("nether_zone.wither_skeleton_drops.detailed_logging", true);

        plugin.getLogger().info("✓ Nether Zone drop rates loaded:");
        plugin.getLogger().info("  - Bone: " + (boneRate * 100) + "%");
        plugin.getLogger().info("  - Coal: " + (coalRate * 100) + "%");
        plugin.getLogger().info("  - Skull: " + (skullRate * 100) + "%");
        plugin.getLogger().info("  - Stone Sword: " + (swordRate * 100) + "%");
        plugin.getLogger().info("  - XP Multiplier: " + (xpMultiplier * 100) + "%");
        plugin.getLogger().info("  - Detailed Logging: " + (detailedLogging ? "ENABLED" : "DISABLED"));
    }

    /**
     * Reduce Wither Skeleton drops in Nether Zone
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onWitherSkeletonDeath(EntityDeathEvent event) {
        // Check if it's a Wither Skeleton
        if (event.getEntityType() != EntityType.WITHER_SKELETON) {
            return;
        }

        // Check if in Nether Zone
        if (!netherZoneManager.isInNetherZone(event.getEntity().getLocation())) {
            return;
        }

        // Check if killed by player (if config requires it)
        if (playerKillOnly && !(event.getEntity().getKiller() instanceof Player)) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        String killerName = killer != null ? killer.getName() : "Unknown";

        // Count original drops
        Map<Material, Integer> originalDrops = new HashMap<>();
        for (ItemStack item : event.getDrops()) {
            originalDrops.put(item.getType(), originalDrops.getOrDefault(item.getType(), 0) + item.getAmount());
        }

        int originalXp = event.getDroppedExp();

        // Reduce drops based on config rates
        Map<Material, Integer> removedDrops = reduceDrops(event);

        // Count remaining drops
        Map<Material, Integer> remainingDrops = new HashMap<>();
        for (ItemStack item : event.getDrops()) {
            remainingDrops.put(item.getType(), remainingDrops.getOrDefault(item.getType(), 0) + item.getAmount());
        }

        // Reduce XP if enabled
        int newXp = originalXp;
        if (reduceXp) {
            newXp = (int) (originalXp * xpMultiplier);
            event.setDroppedExp(newXp);
        }

        // Log results
        if (detailedLogging) {
            logDropReduction(killerName, originalDrops, remainingDrops, removedDrops, originalXp, newXp);
        }

        // Send message to killer (optional)
        if (killer != null && !removedDrops.isEmpty()) {
            killer.sendMessage("§7[Nether Zone] §eSome drops were reduced");
        }
    }

    /**
     * Reduce drops based on config rates
     * Returns: Map of removed items
     */
    private Map<Material, Integer> reduceDrops(EntityDeathEvent event) {
        Map<Material, Integer> removedDrops = new HashMap<>();
        Iterator<ItemStack> iterator = event.getDrops().iterator();

        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            Material type = item.getType();
            int amount = item.getAmount();

            double keepRate = getKeepRate(type);

            // Roll random chance
            if (random.nextDouble() > keepRate) {
                // Remove this drop
                removedDrops.put(type, removedDrops.getOrDefault(type, 0) + amount);
                iterator.remove();
            }
        }

        return removedDrops;
    }

    /**
     * Get keep rate for specific item type
     */
    private double getKeepRate(Material type) {
        switch (type) {
            case BONE:
                return boneRate;
            case COAL:
                return coalRate;
            case WITHER_SKELETON_SKULL:
                return skullRate;
            case STONE_SWORD:
                return swordRate;
            default:
                return 1.0; // Keep other items
        }
    }

    /**
     * Log drop reduction details
     */
    private void logDropReduction(String killerName, Map<Material, Integer> original,
                                  Map<Material, Integer> remaining, Map<Material, Integer> removed,
                                  int originalXp, int newXp) {
        plugin.getLogger().info("═══════════════════════════════════════════");
        plugin.getLogger().info("Wither Skeleton killed by: " + killerName);
        plugin.getLogger().info("");
        plugin.getLogger().info("Original drops:");
        if (original.isEmpty()) {
            plugin.getLogger().info("  - None");
        } else {
            for (Map.Entry<Material, Integer> entry : original.entrySet()) {
                plugin.getLogger().info("  - " + entry.getKey() + " x" + entry.getValue());
            }
        }
        plugin.getLogger().info("");
        plugin.getLogger().info("Removed drops:");
        if (removed.isEmpty()) {
            plugin.getLogger().info("  - None");
        } else {
            for (Map.Entry<Material, Integer> entry : removed.entrySet()) {
                int originalAmount = original.getOrDefault(entry.getKey(), 0);
                int removedAmount = entry.getValue();
                double percentage = (removedAmount * 100.0) / originalAmount;
                plugin.getLogger().info("  - " + entry.getKey() + " x" + removedAmount +
                        " (" + String.format("%.1f", percentage) + "% removed)");
            }
        }
        plugin.getLogger().info("");
        plugin.getLogger().info("Remaining drops:");
        if (remaining.isEmpty()) {
            plugin.getLogger().info("  - None");
        } else {
            for (Map.Entry<Material, Integer> entry : remaining.entrySet()) {
                plugin.getLogger().info("  - " + entry.getKey() + " x" + entry.getValue());
            }
        }
        plugin.getLogger().info("");
        plugin.getLogger().info("XP: " + originalXp + " → " + newXp +
                " (" + String.format("%.1f", (newXp * 100.0) / originalXp) + "%)");
        plugin.getLogger().info("═══════════════════════════════════════════");
    }
}
