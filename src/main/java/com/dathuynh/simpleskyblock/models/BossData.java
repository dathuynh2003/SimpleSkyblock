package com.dathuynh.simpleskyblock.models;

import org.bukkit.entity.IronGolem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossData {

    private IronGolem boss;
    private long lastDeathTime;
    private boolean isAlive;
    private Map<UUID, Double> damageTracker;

    public BossData() {
        this.lastDeathTime = 0;
        this.isAlive = false;
        this.damageTracker = new HashMap<>();
    }

    public IronGolem getBoss() {
        return boss;
    }

    public void setBoss(IronGolem boss) {
        this.boss = boss;
        this.isAlive = true;
    }

    public long getLastDeathTime() {
        return lastDeathTime;
    }

    public void setLastDeathTime(long time) {
        this.lastDeathTime = time;
    }

    /**
     * ← FIX: CHỈ CHECK isAlive FLAG, KHÔNG CHECK boss.isDead()
     * Tránh crash khi boss entity bị unload
     */
    public boolean isAlive() {
        return isAlive && boss != null;
    }

    /**
     * ← NEW: Method riêng để check boss THỰC SỰ còn sống
     * Dùng khi cần verify boss entity
     */
    public boolean isBossEntityValid() {
        if (boss == null) return false;

        try {
            return !boss.isDead() && boss.isValid();
        } catch (Exception e) {
            // Entity unloaded or invalid
            return false;
        }
    }

    public void setAlive(boolean alive) {
        this.isAlive = alive;
    }

    public Map<UUID, Double> getDamageTracker() {
        return damageTracker;
    }

    public void addDamage(UUID playerUUID, double damage) {
        damageTracker.put(playerUUID, damageTracker.getOrDefault(playerUUID, 0.0) + damage);
    }

    public void resetDamageTracker() {
        damageTracker.clear();
    }

    public long getRespawnTimeLeft(int defaultRespawnTime) {
        if (isAlive) return 0;

        long elapsed = System.currentTimeMillis() - lastDeathTime;
        long respawnTime = defaultRespawnTime * 60 * 1000; // 30 minutes
        long remaining = respawnTime - elapsed;

        return Math.max(0, remaining);
    }

    public boolean canRespawn(int defaultRespawnTime) {
        return !isAlive && getRespawnTimeLeft(defaultRespawnTime) == 0;
    }
}
