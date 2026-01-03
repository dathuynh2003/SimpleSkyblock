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

        spawnLoc.getChunk().load();
        spawnLoc.getChunk().setForceLoaded(true);

        IronGolem boss = world.spawn(spawnLoc, IronGolem.class);

        String bossName = bossConfig.getBossName(BOSS_ID);
        double health = bossConfig.getHealth(BOSS_ID);
        double damage = bossConfig.getDamage(BOSS_ID);
        double armor = bossConfig.getArmor(BOSS_ID);
        double armorToughness = bossConfig.getArmorToughness(BOSS_ID);
        double knockbackResist = bossConfig.getKnockbackResistance(BOSS_ID);
        double movementSpeed = bossConfig.getMovementSpeed(BOSS_ID);
        double followRange = bossConfig.getFollowRange(BOSS_ID);

        boss.setCustomName(bossName);
        boss.setCustomNameVisible(true);

        boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
        boss.setHealth(health);
        boss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(damage);
        boss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(movementSpeed);
        boss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(knockbackResist);
        boss.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(armor);
        boss.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).setBaseValue(armorToughness);
        boss.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(followRange);

        Map<PotionEffectType, Integer> effects = bossConfig.getEffects(BOSS_ID);
        for (Map.Entry<PotionEffectType, Integer> entry : effects.entrySet()) {
            boss.addPotionEffect(new PotionEffect(entry.getKey(), 999999, entry.getValue(), false, false));
        }

        boss.setPlayerCreated(false);
        boss.setRemoveWhenFarAway(false);
        boss.setPersistent(true);

        bossData.setBoss(boss);
        bossData.resetDamageTracker();

        if (visibilityListener != null) {
            visibilityListener.clearCache();
        }

        createHealthBossBar();

        List<String> announcement = bossConfig.getSpawnAnnouncement(BOSS_ID);
        for (String line : announcement) {
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', line));
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.8f);
        }

        plugin.getLogger().info("Boss '" + BOSS_ID + "' spawned at Arena1");
    }

    private void createHealthBossBar() {
        String bossName = bossConfig.getBossName(BOSS_ID);

        healthBossBar = Bukkit.createBossBar(
                bossName + " §c❤ §e100%",
                BarColor.RED,
                BarStyle.SEGMENTED_10
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (arenaManager.isInArena(player.getLocation())) {
                healthBossBar.addPlayer(player);
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

                if (percent > 50) {
                    healthBossBar.setColor(BarColor.GREEN);
                } else if (percent > 25) {
                    healthBossBar.setColor(BarColor.YELLOW);
                } else {
                    healthBossBar.setColor(BarColor.RED);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L).getTaskId();
    }

    public void updateBossBarVisibility(Player player) {
        if (healthBossBar == null || !bossData.isAlive()) {
            return;
        }

        boolean inArena = arenaManager.isInArena(player.getLocation());
        boolean hasBar = healthBossBar.getPlayers().contains(player);

        if (inArena && !hasBar) {
            healthBossBar.addPlayer(player);
        } else if (!inArena && hasBar) {
            healthBossBar.removePlayer(player);
        }
    }

    public void handleBossDeath() {
        try {
            Map<UUID, Double> damages = bossData.getDamageTracker();

            if (damages.isEmpty()) {
                bossData.setAlive(false);
                bossData.setLastDeathTime(System.currentTimeMillis());

                List<String> deathAnnouncement = bossConfig.getDeathAnnouncement(BOSS_ID);
                for (String line : deathAnnouncement) {
                    Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', line));
                }
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', bossConfig.getRespawnNotice(BOSS_ID)));
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§8§m                                                    ");
                return;
            }

            List<Map.Entry<UUID, Double>> sortedDamages = damages.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());

            List<String> deathAnnouncement = bossConfig.getDeathAnnouncement(BOSS_ID);
            for (String line : deathAnnouncement) {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', line));
            }

            for (int i = 0; i < sortedDamages.size(); i++) {
                UUID playerUUID = sortedDamages.get(i).getKey();

                int rewardAmount = calculateReward(i, sortedDamages.size());
                String itemId = getRewardItemId(i);
                String rankName = getRankName(i);
                double damage = sortedDamages.get(i).getValue();

                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                String playerName = offlinePlayer.hasPlayedBefore() ? offlinePlayer.getName() : "Unknown";

                if (i < 3) {
                    String msg = String.format("  §e%s §f%s §7- §c%.0f damage §7(+§b%dx phần thưởng§7)",
                            rankName, playerName, damage, rewardAmount);
                    Bukkit.broadcastMessage(msg);
                }

                Player player = Bukkit.getPlayer(playerUUID);

                if (player != null && player.isOnline()) {
                    giveRewardToPlayer(player, itemId, rewardAmount, rankName, damage);
                } else {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        Player delayedPlayer = Bukkit.getPlayer(playerUUID);
                        if (delayedPlayer != null && delayedPlayer.isOnline()) {
                            giveRewardToPlayer(delayedPlayer, itemId, rewardAmount, rankName, damage);
                        }
                    }, 60L);
                }
            }

            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', bossConfig.getRespawnNotice(BOSS_ID)));
            Bukkit.broadcastMessage("");
            Bukkit.broadcastMessage("§8§m                                                    ");

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

        } catch (Exception e) {
            plugin.getLogger().severe("Exception in handleBossDeath()!");
            e.printStackTrace();
        }
    }

    private void giveRewardToPlayer(Player player, String itemId, int rewardAmount, String rank, double damage) {
        try {
            giveReward(player, itemId, rewardAmount);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to give reward to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }

        player.sendMessage("");
        player.sendMessage("§a✓ Bạn đạt §e" + rank + " §asát thương!");
        player.sendMessage("§7Tổng sát thương: §c" + String.format("%.0f", damage));
        player.sendMessage("§7Phần thưởng: §e" + rewardAmount + "x §bphần thưởng");
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK,
                player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
    }

    private int calculateReward(int rank, int totalPlayers) {
        Random random = new Random();

        if (rank == 0) {
            int min = bossConfig.getRank1Min(BOSS_ID);
            int max = bossConfig.getRank1Max(BOSS_ID);
            return min + random.nextInt(max - min + 1);
        } else if (rank == 1 || rank == 2) {
            int min = bossConfig.getRank23Min(BOSS_ID);
            int max = bossConfig.getRank23Max(BOSS_ID);
            return min + random.nextInt(max - min + 1);
        } else {
            return bossConfig.getRankOtherAmount(BOSS_ID);
        }
    }

    private String getRewardItemId(int rank) {
        if (rank == 0) {
            return bossConfig.getRank1ItemId(BOSS_ID);
        } else if (rank == 1 || rank == 2) {
            return bossConfig.getRank23ItemId(BOSS_ID);
        } else {
            return bossConfig.getRankOtherItemId(BOSS_ID);
        }
    }

    private String getRankName(int rank) {
        switch (rank) {
            case 0:
                return "TOP 1";
            case 1:
                return "TOP 2";
            case 2:
                return "TOP 3";
            default:
                return "#" + (rank + 1);
        }
    }

    private void giveReward(Player player, String itemId, int amount) {
        ItemStack reward = itemManager.getItem(itemId);

        if (reward == null) {
            plugin.getLogger().warning("Item '" + itemId + "' not found in items.yml!");
            return;
        }

        reward.setAmount(amount);

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(reward);

        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), reward);
            player.sendMessage("§c Inventory đầy! Items rơi xuống đất.");
        }
    }

    public void trackDamage(Player player, double damage) {
        if (!bossData.isAlive()) return;
        bossData.addDamage(player.getUniqueId(), damage);
    }

    public IronGolem getBoss() {
        return bossData.getBoss();
    }

    public boolean isBossAlive() {
        return bossData.isAlive();
    }

    public BossData getBossData() {
        return bossData;
    }

    public Main getPlugin() {
        return plugin;
    }

    private void startRespawnChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (bossData.canRespawn(bossConfig.getRespawnTime(BOSS_ID))) {
                    spawnBoss();
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L);
    }

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

                if (!bossData.isBossEntityValid()) {
                    bossData.setAlive(false);
                    return;
                }

                if (!boss.getLocation().getChunk().isLoaded()) {
                    boss.getLocation().getChunk().load();
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
        long cooldown = bossConfig.getRangedCooldown(BOSS_ID) * 50L;

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

        boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        boss.getWorld().spawnParticle(Particle.FLAME, eyeLoc, 20, 0.3, 0.3, 0.3, 0.05);
    }

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

    public String getRespawnTimeFormatted() {
        long millis = bossData.getRespawnTimeLeft(bossConfig.getRespawnTime(BOSS_ID));

        if (millis == 0) return "§aSẵn sàng!";

        long minutes = millis / 60000;
        long seconds = (millis % 60000) / 1000;

        return String.format("§e%dm %ds", minutes, seconds);
    }

    public void reloadConfig() {
        bossConfig.reload();
    }

    public void startEnrageExplosions(IronGolem boss) {
        if (enrageExplosionTaskId != -1) {
            return;
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

                world.spawnParticle(Particle.EXPLOSION_LARGE, bossLoc, 10, 2.5, 0.5, 2.5);
                world.playSound(bossLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

                for (Entity entity : world.getNearbyEntities(bossLoc, 5, 3, 5)) {
                    if (entity instanceof Player) {
                        Player victim = (Player) entity;

                        if (victim.getGameMode() == GameMode.CREATIVE ||
                                victim.getGameMode() == GameMode.SPECTATOR) {
                            continue;
                        }

                        double distance = victim.getLocation().distance(bossLoc);
                        double damage = 15 * (1 - (distance / 5));

                        victim.damage(damage, boss);

                        // CHECK KNOCKBACK RESISTANCE
                        double knockbackResist = getPlayerKnockbackResistance(victim);

                        if (knockbackResist < 1.0) {
                            double knockbackMultiplier = (1.0 - knockbackResist) * 1.5;

                            org.bukkit.util.Vector knockback = victim.getLocation()
                                    .subtract(bossLoc)
                                    .toVector()
                                    .normalize()
                                    .multiply(knockbackMultiplier)
                                    .setY(0.5 * (1.0 - knockbackResist));

                            victim.setVelocity(knockback);
                        }

                        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);
                    }
                }

            }
        }.runTaskTimer(plugin, 60L, 60L).getTaskId();
    }

    public void addPlayerToBossBar(Player player) {
        if (healthBossBar != null && bossData.isAlive()) {
            healthBossBar.addPlayer(player);
        }
    }

    public BossConfig getBossConfig() {
        return bossConfig;
    }

    private double getPlayerKnockbackResistance(Player player) {
        double total = 0.0;

        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack item : armor) {
            if (item != null && item.hasItemMeta()) {
                total += getItemKnockbackResistance(item);
            }
        }

        return Math.min(1.0, total);
    }

    private double getItemKnockbackResistance(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0.0;

        try {
            var attributes = item.getItemMeta().getAttributeModifiers(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
            if (attributes == null || attributes.isEmpty()) return 0.0;

            double total = 0.0;
            for (var modifier : attributes) {
                total += modifier.getAmount();
            }
            return total;
        } catch (Exception e) {
            return 0.0;
        }
    }

}
