package com.dathuynh.simpleskyblock.managers;

import com.dathuynh.simpleskyblock.Main;
import com.dathuynh.simpleskyblock.models.BossData;
import com.dathuynh.simpleskyblock.utils.BossConfig;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class BossManager {

    private Main plugin;
    private ArenaManager arenaManager;
    private ItemManager itemManager;
    private BossConfig bossConfig;
    private BossData bossData;

    private int healthBarTaskId = -1;
    private int aggressiveAITaskId = -1;

    private static final String BOSS_ID = "arena1_boss";

    public BossManager(Main plugin, ArenaManager arenaManager, ItemManager itemManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.itemManager = itemManager;
        this.bossConfig = new BossConfig(plugin);
        this.bossData = new BossData();

        startRespawnChecker();
        startHealthBarUpdater();
        startAggressiveAI();
    }

    /**
     * Spawn boss using config
     */
    public void spawnBoss() {
        if (bossData.isAlive()) {
            plugin.getLogger().warning("Boss đã tồn tại!");
            return;
        }

        Location spawnLoc = arenaManager.getArena1Center();
        if (spawnLoc == null) {
            plugin.getLogger().severe("Cannot spawn boss: Arena1 not created!");
            return;
        }

        World world = spawnLoc.getWorld();
        if (world == null) return;

        //  Force load chunk
        spawnLoc.getChunk().load();
        spawnLoc.getChunk().setForceLoaded(true);
        plugin.getLogger().info("✅ Boss chunk force-loaded!");

        // Spawn Iron Golem
        IronGolem boss = world.spawn(spawnLoc, IronGolem.class);

        // Load config
        String bossName = bossConfig.getBossName(BOSS_ID);
        double health = bossConfig.getHealth(BOSS_ID);
        double damage = bossConfig.getDamage(BOSS_ID);
        double armor = bossConfig.getArmor(BOSS_ID);
        double armorToughness = bossConfig.getArmorToughness(BOSS_ID);
        double knockbackResist = bossConfig.getKnockbackResistance(BOSS_ID);
        double movementSpeed = bossConfig.getMovementSpeed(BOSS_ID);
        double followRange = bossConfig.getFollowRange(BOSS_ID);

        // Set custom name
        boss.setCustomName(bossName);
        boss.setCustomNameVisible(true);

        // Set attributes
        boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
        boss.setHealth(health);
        boss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        boss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(movementSpeed);
        boss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(knockbackResist);
        boss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
        boss.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(armorToughness);
        boss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(followRange);

        // Apply potion effects from config
        Map<PotionEffectType, Integer> effects = bossConfig.getEffects(BOSS_ID);
        for (Map.Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
            boss.addPotionEffect(new PotionEffect(entry.getKey(), 999999, entry.getValue(), false, false));
        }

        // AI Settings
        boss.setPlayerCreated(false);
        boss.setRemoveWhenFarAway(false);
        boss.setPersistent(true);

        // Save boss data
        bossData.setBoss(boss);
        bossData.resetDamageTracker();

        // Announce spawn from config
        List<String> announcement = bossConfig.getSpawnAnnouncement(BOSS_ID);
        for (String line : announcement) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', line));
        }

        // Sound effect for all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        }

        plugin.getLogger().info("Boss '" + BOSS_ID + "' spawned at Arena1 with follow-range=" + followRange);
    }

    /**
     * Handle boss death - FIX NULL PLAYER BUG
     */
    public void handleBossDeath() {
        try {
            plugin.getLogger().info("═══════════════════════════════════");
            plugin.getLogger().info("[BossManager] handleBossDeath() START");
            plugin.getLogger().info("═══════════════════════════════════");

            // Get damage tracker
            Map<UUID, Double> damages = bossData.getDamageTracker();
            plugin.getLogger().info("[DEBUG] Damage tracker size: " + damages.size());

            // Log all damage data
            for (Map.Entry<UUID, Double> entry : damages.entrySet()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
                String name = op.hasPlayedBefore() ? op.getName() : "Unknown";
                plugin.getLogger().info("[DEBUG] - " + name + " (" + entry.getKey() + "): " + entry.getValue() + " damage");
            }

            if (damages.isEmpty()) {
                plugin.getLogger().warning("[WARNING] Boss died with NO damage trackers!");
                bossData.setAlive(false);
                bossData.setLastDeathTime(System.currentTimeMillis());

                // Still announce
                List<String> deathAnnouncement = bossConfig.getDeathAnnouncement(BOSS_ID);
                for (String line : deathAnnouncement) {
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', line));
                }
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                        bossConfig.getRespawnNotice(BOSS_ID)));
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8§m                                                    ");

                plugin.getLogger().info("[BossManager] handleBossDeath() END (no trackers)");
                return;
            }

            // Sort by damage (descending)
            List<Map.Entry<UUID, Double>> sortedDamages = damages.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());

            // Announce death + rankings
            List<String> deathAnnouncement = bossConfig.getDeathAnnouncement(BOSS_ID);
            for (String line : deathAnnouncement) {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', line));
            }

            // Distribute rewards + announce top 3
            for (int i = 0; i < sortedDamages.size(); i++) {
                UUID playerUUID = sortedDamages.get(i).getKey();

                int rewardAmount = calculateReward(i, sortedDamages.size());
                String itemId = getRewardItemId(i);
                String rankName = getRankName(i);
                double damage = sortedDamages.get(i).getValue();

                // Get name from OfflinePlayer (always works)
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                String playerName = offlinePlayer.hasPlayedBefore() ? offlinePlayer.getName() : "Unknown";

                plugin.getLogger().info("[DEBUG] #" + (i + 1) + " " + playerName + " - " + damage + " damage - " + rewardAmount + "x " + itemId);

                // Announce top 3 publicly
                if (i < 3) {
                    String msg = String.format("  §e%s §f%s §7- §c%.0f damage §7(+§b%dx phần thưởng§7)",
                            rankName, playerName, damage, rewardAmount);
                    Bukkit.broadcastMessage(msg);
                }

                // Give rewards with retry mechanism
                Player player = Bukkit.getPlayer(playerUUID);

                if (player != null && player.isOnline()) {
                    // Player online → give immediately
                    giveRewardToPlayer(player, itemId, rewardAmount, rankName, damage);
                } else {
                    // Player null/offline → DELAY 3 seconds
                    plugin.getLogger().warning("[DEBUG] Player " + playerName + " is NULL/OFFLINE, delaying reward by 3s...");

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Player delayedPlayer = Bukkit.getPlayer(playerUUID);
                        if (delayedPlayer != null && delayedPlayer.isOnline()) {
                            plugin.getLogger().info("[DEBUG] ✅ Delayed reward given to " + delayedPlayer.getName());
                            giveRewardToPlayer(delayedPlayer, itemId, rewardAmount, rankName, damage);
                        } else {
                            plugin.getLogger().severe("[ERROR] ❌ Player " + playerUUID + " still offline after delay!");
                        }
                    }, 60L); // 3 seconds = 60 ticks
                }
            }

            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                    bossConfig.getRespawnNotice(BOSS_ID)));
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§8§m                                                    ");

            // Update boss data
            bossData.setAlive(false);
            bossData.setLastDeathTime(System.currentTimeMillis());
            bossData.resetDamageTracker();

            plugin.getLogger().info("[BossManager] handleBossDeath() END (success)");
            plugin.getLogger().info("═══════════════════════════════════");

        } catch (Exception e) {
            plugin.getLogger().severe("[ERROR] Exception in handleBossDeath()!");
            e.printStackTrace();
        }
    }

    /**
     * Give reward + messages to player (extracted method)
     */
    private void giveRewardToPlayer(Player player, String itemId, int rewardAmount, String rank, double damage) {
        try {
            giveReward(player, itemId, rewardAmount);
            plugin.getLogger().info("[DEBUG] ✅ Rewards given successfully to " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().severe("[ERROR] Failed to give reward to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        // Private message
        player.sendMessage("");
        player.sendMessage("§a✓ Bạn đạt §e" + rank + " §asát thương!");
        player.sendMessage("§7Tổng sát thương: §c" + String.format("%.0f", damage));
        player.sendMessage("§7Phần thưởng: §e" + rewardAmount + "x §bphần thưởng");
        player.sendMessage("");

        // Sound + particles
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK,
                player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
    }

    /**
     * Calculate reward based on rank (from config)
     */
    private int calculateReward(int rank, int totalPlayers) {
        Random random = new Random();

        if (rank == 0) {
            // Top 1
            int min = bossConfig.getRank1Min(BOSS_ID);
            int max = bossConfig.getRank1Max(BOSS_ID);
            return min + random.nextInt(max - min + 1);
        } else if (rank == 1 || rank == 2) {
            // Top 2-3
            int min = bossConfig.getRank23Min(BOSS_ID);
            int max = bossConfig.getRank23Max(BOSS_ID);
            return min + random.nextInt(max - min + 1);
        } else {
            // Others
            return bossConfig.getRankOtherAmount(BOSS_ID);
        }
    }

    /**
     * Get reward item ID from config
     */
    private String getRewardItemId(int rank) {
        if (rank == 0) {
            return bossConfig.getRank1ItemId(BOSS_ID);
        } else if (rank == 1 || rank == 2) {
            return bossConfig.getRank23ItemId(BOSS_ID);
        } else {
            return bossConfig.getRankOtherItemId(BOSS_ID);
        }
    }

    /**
     * Get rank display name
     */
    private String getRankName(int rank) {
        switch (rank) {
            case 0:
                return "🥇 TOP 1";
            case 1:
                return "🥈 TOP 2";
            case 2:
                return "🥉 TOP 3";
            default:
                return "#" + (rank + 1);
        }
    }

    /**
     * Give reward item from ItemManager
     */
    private void giveReward(Player player, String itemId, int amount) {
        ItemStack reward = itemManager.getItem(itemId);

        if (reward == null) {
            plugin.getLogger().warning("Item '" + itemId + "' not found in items.yml!");
            return;
        }

        reward.setAmount(amount);

        // Give to player
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(reward);

        if (!leftover.isEmpty()) {
            // Drop if inventory full
            player.getWorld().dropItemNaturally(player.getLocation(), reward);
            player.sendMessage("§c⚠ Inventory đầy! Items rơi xuống đất.");
        }
    }

    /**
     * Track damage from player to boss
     */
    public void trackDamage(Player player, double damage) {
        if (!bossData.isAlive()) return;
        bossData.addDamage(player.getUniqueId(), damage);
        plugin.getLogger().info("[DEBUG] ✅ Tracked " + String.format("%.2f", damage) + " damage from " + player.getName() +
                " (Total: " + String.format("%.2f", bossData.getDamageTracker().get(player.getUniqueId())) + ")");
    }

    /**
     * Get current boss
     */
    public IronGolem getBoss() {
        return bossData.getBoss();
    }

    /**
     * Check if boss is alive
     */
    public boolean isBossAlive() {
        return bossData.isAlive();
    }

    /**
     * Get boss data
     */
    public BossData getBossData() {
        return bossData;
    }

    /**
     * Get plugin instance
     */
    public Main getPlugin() {
        return plugin;
    }

    /**
     * Auto respawn checker (every 1 minute)
     */
    private void startRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (bossData.canRespawn()) {
                    spawnBoss();
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L);
    }

    /**
     * Health bar updater - FIX: Keep boss chunk loaded
     */
    private void startHealthBarUpdater() {
        healthBarTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!bossData.isAlive()) return;

                IronGolem boss = bossData.getBoss();
                if (boss == null) {
                    bossData.setAlive(false);
                    return;
                }

                // ← FIX: Check if boss entity is valid
                if (!bossData.isBossEntityValid()) {
                    plugin.getLogger().warning("[BossManager] ⚠️ Boss entity invalid! Marking as dead.");
                    bossData.setAlive(false);
                    return;
                }

                // ← FIX: Keep chunk loaded
                if (!boss.getLocation().getChunk().isLoaded()) {
                    boss.getLocation().getChunk().load();
                    plugin.getLogger().info("[BossManager] 🔄 Loaded boss chunk");
                }

                double health = boss.getHealth();
                double maxHealth = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();

                double healthPercent = (health / maxHealth) * 100;
                String healthBar = String.format("§c§l⚔ Khổng lồ sắt §c❤ §e%.0f§7/§e%.0f §7(§c%.1f%%§7)",
                        health, maxHealth, healthPercent);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (arenaManager.isInArena(player.getLocation())) {
                        player.spigot().sendMessage(
                                net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent(healthBar)
                        );
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    /**
     * Aggressive AI - Boss actively targets players
     * ← FIX: CHỈ FIND TARGET, KHÔNG DAMAGE (để BossListener xử lý)
     */
    private void startAggressiveAI() {
        aggressiveAITaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!bossData.isAlive()) return;
                if (!bossConfig.isAggressive(BOSS_ID)) return;

                IronGolem boss = bossData.getBoss();
                if (boss == null || boss.isDead()) return;

                // Find & target nearest player
                Player currentTarget = null;

                // Check existing target
                if (boss.getTarget() != null && boss.getTarget() instanceof Player) {
                    currentTarget = (Player) boss.getTarget();
                    if (!currentTarget.isOnline() || currentTarget.isDead() ||
                            currentTarget.getGameMode() == GameMode.CREATIVE ||
                            currentTarget.getGameMode() == GameMode.SPECTATOR) {
                        currentTarget = null; // Invalid target
                    }
                }

                // Find new target if needed
                if (currentTarget == null) {
                    double targetRange = bossConfig.getTargetRange(BOSS_ID);
                    Location bossLoc = boss.getLocation();
                    Player nearestPlayer = null;
                    double nearestDistance = targetRange;

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getGameMode() == GameMode.CREATIVE ||
                                player.getGameMode() == GameMode.SPECTATOR) {
                            continue;
                        }

                        if (!player.getWorld().equals(boss.getWorld())) continue;

                        double distance = player.getLocation().distance(bossLoc);
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            nearestPlayer = player;
                        }
                    }

                    // Set new target
                    if (nearestPlayer != null) {
                        boss.setTarget(nearestPlayer);
                    }
                }

                // ← NOTE: KHÔNG DAMAGE Ở ĐÂY!
                // Attack được handle bởi BossListener.onBossAttack()
                // → Damage được track đúng qua EntityDamageByEntityEvent
            }
        }.runTaskTimer(plugin, 20L, 40L).getTaskId(); // Every 2 seconds
    }

    /**
     * Stop tasks on disable
     */
    public void shutdown() {
        if (healthBarTaskId != -1) {
            Bukkit.getScheduler().cancelTask(healthBarTaskId);
        }
        if (aggressiveAITaskId != -1) {
            Bukkit.getScheduler().cancelTask(aggressiveAITaskId);
        }
    }

    /**
     * Get respawn time formatted
     */
    public String getRespawnTimeFormatted() {
        long millis = bossData.getRespawnTimeLeft();

        if (millis == 0) return "§aSẵn sàng!";

        long minutes = millis / 60000;
        long seconds = (millis % 60000) / 1000;

        return String.format("§e%dm %ds", minutes, seconds);
    }

    /**
     * Reload boss config
     */
    public void reloadConfig() {
        bossConfig.reload();
    }
}
