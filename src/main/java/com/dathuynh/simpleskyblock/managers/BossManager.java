package com.dathuynh.simpleskyblock.managers;

import com.dathuynh.simpleskyblock.Main;
import com.dathuynh.simpleskyblock.listeners.BossBarVisibilityListener;
import com.dathuynh.simpleskyblock.models.BossData;
import com.dathuynh.simpleskyblock.utils.BossConfig;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
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

    private BossBar healthBossBar;
    private int bossBarTaskId = -1;

    private int enrageExplosionTaskId = -1;

    private BossBarVisibilityListener visibilityListener;

    public void setVisibilityListener(BossBarVisibilityListener listener) {
        this.visibilityListener = listener;
    }

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

        // Force load chunk
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

        // ✅ FIX: Clear cache TRƯỚC KHI tạo BossBar
        if (visibilityListener != null) {
            visibilityListener.clearCache();
        }

        // Create BossBar for health display
        createHealthBossBar();

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

    private void createHealthBossBar() {
        String bossName = bossConfig.getBossName(BOSS_ID);

        healthBossBar = Bukkit.createBossBar(
                bossName + " §c❤ §e100%",
                BarColor.RED,
                BarStyle.SEGMENTED_10
        );

        plugin.getLogger().info("[BossBar] === Checking players for initial BossBar ===");

        //Khởi tạo: Chỉ add player đang ở arena
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation();
            boolean inArena = arenaManager.isInArena(loc);

            plugin.getLogger().info("[BossBar] Player: " + player.getName() +
                    " | Loc: " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() +
                    " | InArena: " + inArena);

            if (inArena) {
                healthBossBar.addPlayer(player);
                plugin.getLogger().info("[BossBar] ✅ Added " + player.getName() + " to BossBar");
            } else {
                plugin.getLogger().info("[BossBar] ⏭ Skipped " + player.getName() + " (not in arena)");
            }
        }

        healthBossBar.setVisible(true);
        healthBossBar.setProgress(1.0);

        startBossBarUpdater();
    }

    private void startBossBarUpdater() {
        bossBarTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!bossData.isAlive() || healthBossBar == null) {
                    if (healthBossBar != null) {
                        healthBossBar.removeAll();
                        healthBossBar = null;
                    }
                    cancel();
                    return;
                }

                IronGolem boss = bossData.getBoss();
                if (boss == null || boss.isDead()) {
                    healthBossBar.removeAll();
                    healthBossBar = null;
                    cancel();
                    return;
                }

                double health = boss.getHealth();
                double maxHealth = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                double progress = health / maxHealth;
                double percent = progress * 100;

                String bossName = bossConfig.getBossName(BOSS_ID);
                healthBossBar.setTitle(String.format("%s §c❤ §e%.0f§7/§e%.0f §7(§c%.1f%%§7)",
                        bossName, health, maxHealth, percent));
                healthBossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

                // Update color based on health
                if (percent > 50) {
                    healthBossBar.setColor(BarColor.GREEN);
                } else if (percent > 25) {
                    healthBossBar.setColor(BarColor.YELLOW);
                } else {
                    healthBossBar.setColor(BarColor.RED);
                }

            }
        }.runTaskTimer(plugin, 0L, 10L).getTaskId(); // Update every 0.5s
    }

    public void updateBossBarVisibility(Player player) {
        if (healthBossBar == null || !bossData.isAlive()) {
            return;
        }

        boolean inArena = arenaManager.isInArena(player.getLocation());
        boolean hasBar = healthBossBar.getPlayers().contains(player);

        if (inArena && !hasBar) {
            // Vào arena → add BossBar
            healthBossBar.addPlayer(player);
            plugin.getLogger().info("[BossBar] ✅ Added " + player.getName());
        } else if (!inArena && hasBar) {
            // Rời arena → remove BossBar
            healthBossBar.removePlayer(player);
            plugin.getLogger().info("[BossBar] ❌ Removed " + player.getName());
        }
    }

    // Add players when they join
    public void addPlayerToBossBar(Player player) {
        if (healthBossBar != null && bossData.isAlive()) {
            if (arenaManager.isInArena(player.getLocation())) {
                healthBossBar.addPlayer(player);
            }
        }
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
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', bossConfig.getRespawnNotice(BOSS_ID)));
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§8§m                                                    ");

            // Update boss data
            bossData.setAlive(false);
            bossData.setLastDeathTime(System.currentTimeMillis());
            bossData.resetDamageTracker();

            if (healthBossBar != null) {
                healthBossBar.removeAll();
                healthBossBar.setVisible(false);
                healthBossBar = null;
            }

            if (visibilityListener != null) {
                visibilityListener.clearCache();
            }

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
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    private Map<UUID, Long> lastRangedAttack = new HashMap<>();

    private void startAggressiveAI() {
        aggressiveAITaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!bossData.isAlive()) return;
                if (!bossConfig.isAggressive(BOSS_ID)) return;

                IronGolem boss = bossData.getBoss();
                if (boss == null || boss.isDead()) return;

                Player target = findNearestTarget(boss);

                if (target != null) {
                    boss.setTarget(target);

                    double distance = target.getLocation().distance(boss.getLocation());

                    // Use ranged attack if too far
                    if (distance > 5 && distance <= bossConfig.getRangedRange(BOSS_ID)) {
                        if (canUseRangedAttack(boss)) {
                            performRangedAttack(boss, target);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 40L).getTaskId();
    }

    private Player findNearestTarget(IronGolem boss) {
        if (boss.getTarget() instanceof Player) {
            Player current = (Player) boss.getTarget();
            if (current.isOnline() && !current.isDead() &&
                    current.getGameMode() != GameMode.CREATIVE &&
                    current.getGameMode() != GameMode.SPECTATOR &&
                    current.getWorld().equals(boss.getWorld())) {
                return current;
            }
        }

        double targetRange = bossConfig.getTargetRange(BOSS_ID);
        Location bossLoc = boss.getLocation();
        Player nearest = null;
        double nearestDist = targetRange;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.CREATIVE ||
                    p.getGameMode() == GameMode.SPECTATOR) continue;
            if (!p.getWorld().equals(boss.getWorld())) continue;

            double dist = p.getLocation().distance(bossLoc);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = p;
            }
        }

        return nearest;
    }

    private boolean canUseRangedAttack(IronGolem boss) {
        if (!bossConfig.isRangedAttackEnabled(BOSS_ID)) return false;

        UUID bossId = boss.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = bossConfig.getRangedCooldown(BOSS_ID) * 50L; // ticks to ms

        if (!lastRangedAttack.containsKey(bossId)) {
            lastRangedAttack.put(bossId, now);
            return true;
        }

        long last = lastRangedAttack.get(bossId);
        if (now - last >= cooldown) {
            lastRangedAttack.put(bossId, now);
            return true;
        }

        return false;
    }

    private void performRangedAttack(IronGolem boss, Player target) {
        Location eyeLoc = boss.getEyeLocation();
        org.bukkit.util.Vector direction = target.getEyeLocation()
                .subtract(eyeLoc)
                .toVector()
                .normalize();

        Snowball projectile = boss.getWorld().spawn(eyeLoc, Snowball.class);
        projectile.setShooter(boss);
        projectile.setVelocity(direction.multiply(bossConfig.getRangedRange(BOSS_ID) / 10));
        projectile.setCustomName("§cBoss Fireball");

        // Visual effects
        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        boss.getWorld().spawnParticle(Particle.FLAME, eyeLoc, 20, 0.3, 0.3, 0.3, 0.05);
    }

    /**
     * Stop tasks on disable
     */
    public void shutdown() {
        if (healthBarTaskId != -1) Bukkit.getScheduler().cancelTask(healthBarTaskId);
        if (aggressiveAITaskId != -1) Bukkit.getScheduler().cancelTask(aggressiveAITaskId);
        if (bossBarTaskId != -1) Bukkit.getScheduler().cancelTask(bossBarTaskId);
        if (enrageExplosionTaskId != -1) Bukkit.getScheduler().cancelTask(enrageExplosionTaskId);

        if (healthBossBar != null) {
            healthBossBar.removeAll();
            healthBossBar = null;
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

    public void startEnrageExplosions(IronGolem boss) {
        if (enrageExplosionTaskId != -1) {
            return; // Already active
        }

        enrageExplosionTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (!bossData.isAlive() || boss.isDead()) {
                    cancel();
                    enrageExplosionTaskId = -1;
                    return;
                }

                Location bossLoc = boss.getLocation();
                World world = bossLoc.getWorld();

                // Explosion effect without terrain damage
                world.spawnParticle(Particle.EXPLOSION_LARGE, bossLoc, 10, 2.5, 0.5, 2.5);
                world.playSound(bossLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

                // Damage nearby players in 5x5 area
                for (Entity entity : world.getNearbyEntities(bossLoc, 5, 3, 5)) {
                    if (entity instanceof Player) {
                        Player victim = (Player) entity;

                        if (victim.getGameMode() == GameMode.CREATIVE ||
                                victim.getGameMode() == GameMode.SPECTATOR) {
                            continue;
                        }

                        double distance = victim.getLocation().distance(bossLoc);
                        double damage = 15 * (1 - (distance / 5)); // 15 damage at center, scaling down

                        victim.damage(damage, boss);
                        victim.setVelocity(victim.getLocation().subtract(bossLoc)
                                .toVector().normalize().multiply(1.5).setY(0.5));

                        victim.sendMessage("§c§l⚡ Boss Enrage Explosion!");
                        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);
                    }
                }
            }
        }.runTaskTimer(plugin, 60L, 60L).getTaskId(); // Every 3 seconds
    }

    public BossConfig getBossConfig() {
        return bossConfig;
    }
}
