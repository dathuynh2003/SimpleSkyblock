package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.managers.ArenaManager;
import com.dathuynh.simpleskyblock.managers.BossManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossListener implements Listener {

    private BossManager bossManager;
    private ArenaManager arenaManager;

    // ← Track last damager for each boss hit
    private Map<UUID, UUID> lastDamager = new HashMap<>(); // Boss UUID → Player UUID
    private Map<UUID, Double> lastHealthBefore = new HashMap<>(); // Boss UUID → Health before

    public BossListener(BossManager bossManager, ArenaManager arenaManager) {
        this.bossManager = bossManager;
        this.arenaManager = arenaManager;
    }

    /**
     * Track damage ATTEMPT (before calculation)
     * ← PRIORITY: LOWEST (runs first, before other plugins)
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBossDamageAttempt(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) return;

        IronGolem damaged = (IronGolem) event.getEntity();
        IronGolem boss = bossManager.getBoss();

        if (boss == null || !damaged.equals(boss)) return;

        // Get player damager
        Player player = null;

        if (event.getDamager() instanceof Player) {
            player = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile projectile = (Projectile) event.getDamager();
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                player = (Player) shooter;
            }
        }

        if (player == null) return;

        // ← Store last damager + health BEFORE damage
        lastDamager.put(boss.getUniqueId(), player.getUniqueId());
        lastHealthBefore.put(boss.getUniqueId(), boss.getHealth());

        Bukkit.getLogger().info("[DEBUG] 🎯 Damage attempt by " + player.getName() +
                " - Boss HP before: " + String.format("%.2f", boss.getHealth()) +
                " - Event cancelled: " + event.isCancelled());
    }

    /**
     * Track ACTUAL damage (after calculation)
     * ← PRIORITY: MONITOR (runs last, after all plugins)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBossDamageActual(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) return;

        IronGolem damaged = (IronGolem) event.getEntity();
        IronGolem boss = bossManager.getBoss();

        if (boss == null || !damaged.equals(boss)) return;

        UUID bossUUID = boss.getUniqueId();

        // Check if we tracked a damager
        if (!lastDamager.containsKey(bossUUID)) {
            Bukkit.getLogger().warning("[DEBUG] ⚠️ Damage without tracked damager!");
            return;
        }

        UUID playerUUID = lastDamager.get(bossUUID);
        Player player = Bukkit.getPlayer(playerUUID);

        if (player == null) {
            Bukkit.getLogger().warning("[DEBUG] ⚠️ Player offline: " + playerUUID);
            lastDamager.remove(bossUUID);
            return;
        }

        // ← Wait 1 tick to get ACTUAL health change
        Bukkit.getScheduler().runTaskLater(bossManager.getPlugin(), () -> {
            if (boss.isDead()) {
                Bukkit.getLogger().info("[DEBUG] ☠️ Boss died before tracking!");
                return;
            }

            double healthBefore = lastHealthBefore.getOrDefault(bossUUID, boss.getHealth());
            double healthAfter = boss.getHealth();
            double actualDamage = healthBefore - healthAfter;

            if (actualDamage > 0) {
                bossManager.trackDamage(player, actualDamage);
                Bukkit.getLogger().info("[DEBUG] ✅ ACTUAL damage: " +
                        String.format("%.2f", actualDamage) + " by " + player.getName());
            } else {
                Bukkit.getLogger().warning("[DEBUG] ⚠️ No HP change! " +
                        "Before: " + String.format("%.2f", healthBefore) +
                        " After: " + String.format("%.2f", healthAfter));
            }

            // Check enrage
            double maxHealth = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            double healthPercent = (healthAfter / maxHealth) * 100;

            // Trong onBossDamageActual(), thay thế phần enrage check:
            if (healthPercent < 25 && !boss.hasPotionEffect(PotionEffectType.SPEED)) {
                boss.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 1));
                boss.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 999999, 1));

                Bukkit.broadcastMessage("§c§l[BOSS] §eKhổng lồ sắt đã BỘC PHÁT!");
                Bukkit.broadcastMessage("§7Boss tăng tốc độ và sát thương!");

                boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
                boss.getWorld().spawnParticle(Particle.LAVA, boss.getLocation(), 100, 2, 2, 2);

                // Start enrage explosion task
                bossManager.startEnrageExplosions(boss);
            }

            // Cleanup
            lastDamager.remove(bossUUID);
            lastHealthBefore.remove(bossUUID);

        }, 1L); // Wait 1 tick
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBossEffectApply(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) return;

        IronGolem golem = (IronGolem) event.getEntity();
        IronGolem boss = bossManager.getBoss();

        if (boss == null || !golem.equals(boss)) return;

        // Block negative effects
        PotionEffectType type = event.getModifiedType();
        if (type != null && (
                type.equals(PotionEffectType.SLOW) ||
                        type.equals(PotionEffectType.SLOW_DIGGING) ||
                        type.equals(PotionEffectType.WEAKNESS) ||
                        type.equals(PotionEffectType.POISON) ||
                        type.equals(PotionEffectType.WITHER)
        )) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBossKnockback(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) return;

        IronGolem boss = bossManager.getBoss();
        if (boss != null && event.getEntity().equals(boss)) {
            // Already immune via KNOCKBACK_RESISTANCE = 1.0
            // But ensure no velocity change
            Bukkit.getScheduler().runTask(bossManager.getPlugin(), () -> {
                boss.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            });
        }
    }

    /**
     * Handle boss death
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBossDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (!(entity instanceof IronGolem)) return;

        IronGolem golem = (IronGolem) entity;

        if (golem.getCustomName() == null || !golem.getCustomName().contains("Khổng lồ sắt")) {
            return;
        }

        IronGolem boss = bossManager.getBoss();
        if (boss != null && !golem.equals(boss)) {
            return;
        }

        Bukkit.getLogger().info("[BossListener] ☠️ Boss death detected! Processing rewards...");

        // Cleanup tracking maps
        lastDamager.remove(golem.getUniqueId());
        lastHealthBefore.remove(golem.getUniqueId());

        event.getDrops().clear();
        event.setDroppedExp(0);

        Location loc = golem.getLocation();
        loc.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, loc, 5, 2, 2, 2);
        loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 1.0f);

        Bukkit.getScheduler().runTaskLater(bossManager.getPlugin(), () -> {
            bossManager.handleBossDeath();
        }, 1L);
    }

    @EventHandler
    public void onBossProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;

        Snowball snowball = (Snowball) event.getEntity();
        if (snowball.getCustomName() == null ||
                !snowball.getCustomName().contains("Boss Fireball")) return;

        if (!(snowball.getShooter() instanceof IronGolem)) return;

        if (event.getHitEntity() instanceof Player) {
            Player victim = (Player) event.getHitEntity();
            double damage = bossManager.getBossConfig().getRangedDamage("arena1_boss");

            victim.damage(damage);
            victim.setFireTicks(60);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 40, 1));

            // Effects
            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f);
            victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation(), 30, 0.5, 0.5, 0.5);
        }
    }

}
