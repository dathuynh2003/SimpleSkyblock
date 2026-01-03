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

    private Map<UUID, UUID> lastDamager = new HashMap<>();
    private Map<UUID, Double> lastHealthBefore = new HashMap<>();

    public BossListener(BossManager bossManager, ArenaManager arenaManager) {
        this.bossManager = bossManager;
        this.arenaManager = arenaManager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onBossDamageAttempt(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) return;

        IronGolem damaged = (IronGolem) event.getEntity();
        IronGolem boss = bossManager.getBoss();

        if (boss == null || !damaged.equals(boss)) return;

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

        lastDamager.put(boss.getUniqueId(), player.getUniqueId());
        lastHealthBefore.put(boss.getUniqueId(), boss.getHealth());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBossDamageActual(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) return;

        IronGolem damaged = (IronGolem) event.getEntity();
        IronGolem boss = bossManager.getBoss();

        if (boss == null || !damaged.equals(boss)) return;

        UUID bossUUID = boss.getUniqueId();

        if (!lastDamager.containsKey(bossUUID)) {
            return;
        }

        UUID playerUUID = lastDamager.get(bossUUID);
        Player player = Bukkit.getPlayer(playerUUID);

        if (player == null) {
            lastDamager.remove(bossUUID);
            return;
        }

        Bukkit.getScheduler().runTaskLater(bossManager.getPlugin(), () -> {
            if (boss.isDead()) {
                return;
            }

            double healthBefore = lastHealthBefore.getOrDefault(bossUUID, boss.getHealth());
            double healthAfter = boss.getHealth();
            double actualDamage = healthBefore - healthAfter;

            if (actualDamage > 0) {
                bossManager.trackDamage(player, actualDamage);
            }

            double maxHealth = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            double healthPercent = (healthAfter / maxHealth) * 100;

            if (bossManager.getBossConfig().isEnrageEnabled("arena1_boss") &&
                    healthPercent < bossManager.getBossConfig().getEnrageHealthPercent("arena1_boss") &&
                    !boss.hasPotionEffect(PotionEffectType.SPEED)) {

                Map<PotionEffectType, Integer> enrageEffects = bossManager.getBossConfig().getEnrageEffects("arena1_boss");
                for (Map.Entry<PotionEffectType, Integer> entry : enrageEffects.entrySet()) {
                    boss.addPotionEffect(new PotionEffect(entry.getKey(), 999999, entry.getValue()));
                }

                String enrageMsg = bossManager.getBossConfig().getEnrageBroadcast("arena1_boss");
                Bukkit.broadcastMessage(enrageMsg);

                boss.getWorld().playSound(boss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
                boss.getWorld().spawnParticle(Particle.LAVA, boss.getLocation(), 100, 2, 2, 2);

                bossManager.startEnrageExplosions(boss);
            }

            lastDamager.remove(bossUUID);
            lastHealthBefore.remove(bossUUID);

        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBossEffectApply(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof IronGolem)) return;

        IronGolem golem = (IronGolem) event.getEntity();
        IronGolem boss = bossManager.getBoss();

        if (boss == null || !golem.equals(boss)) return;

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
            Bukkit.getScheduler().runTask(bossManager.getPlugin(), () -> {
                boss.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
            });
        }
    }

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

            victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f);
            victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation(), 30, 0.5, 0.5, 0.5);
        }
    }
}
