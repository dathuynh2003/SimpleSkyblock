package com.dathuynh.simpleskyblock.utils;

import com.dathuynh.simpleskyblock.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.InputStream;
import java.util.*;

public class BossConfig {

    private Main plugin;
    private File file;
    private YamlConfiguration config;

    public BossConfig(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bosses.yml");

        // Extract default if not exists
        if (!file.exists()) {
            plugin.saveResource("bosses.yml", false);
        }

        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }

    // Get boss section
    public ConfigurationSection getBoss(String bossId) {
        return config.getConfigurationSection("bosses." + bossId);
    }

    // Helper methods
    public String getBossName(String bossId) {
        return config.getString("bosses." + bossId + ".name", "§cUnknown Boss");
    }

    public double getHealth(String bossId) {
        return config.getDouble("bosses." + bossId + ".health", 2048);
    }

    public double getDamage(String bossId) {
        return config.getDouble("bosses." + bossId + ".damage", 10);
    }

    public double getArmor(String bossId) {
        return config.getDouble("bosses." + bossId + ".armor", 20);
    }

    public double getArmorToughness(String bossId) {
        return config.getDouble("bosses." + bossId + ".armor-toughness", 12);
    }

    public double getKnockbackResistance(String bossId) {
        return config.getDouble("bosses." + bossId + ".knockback-resistance", 1.0);
    }

    public double getMovementSpeed(String bossId) {
        return config.getDouble("bosses." + bossId + ".movement-speed", 0.18);
    }

    public boolean isAggressive(String bossId) {
        return config.getBoolean("bosses." + bossId + ".aggressive", true);
    }

    public double getTargetRange(String bossId) {
        return config.getDouble("bosses." + bossId + ".target-range", 32);
    }

    public Map<PotionEffectType, Integer> getEffects(String bossId) {
        Map<PotionEffectType, Integer> effects = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("bosses." + bossId + ".effects");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                PotionEffectType type = PotionEffectType.getByName(key);
                if (type != null) {
                    effects.put(type, section.getInt(key) - 1); // Level 2 → amplifier 1
                }
            }
        }

        return effects;
    }

    public int getRespawnTime(String bossId) {
        return config.getInt("bosses." + bossId + ".respawn-time", 30);
    }

    public List<String> getSpawnAnnouncement(String bossId) {
        return config.getStringList("bosses." + bossId + ".announcements.spawn");
    }

    public List<String> getDeathAnnouncement(String bossId) {
        return config.getStringList("bosses." + bossId + ".announcements.death");
    }

    public String getRespawnNotice(String bossId) {
        return config.getString("bosses." + bossId + ".announcements.respawn-notice", "§7Boss respawns soon!");
    }

    // Rewards
    public int getRank1Min(String bossId) {
        return config.getInt("bosses." + bossId + ".rewards.rank-1.min", 3);
    }

    public int getRank1Max(String bossId) {
        return config.getInt("bosses." + bossId + ".rewards.rank-1.max", 5);
    }

    public String getRank1ItemId(String bossId) {
        return config.getString("bosses." + bossId + ".rewards.rank-1.item-id", "super_iron");
    }

    public int getRank23Min(String bossId) {
        return config.getInt("bosses." + bossId + ".rewards.rank-2-3.min", 2);
    }

    public int getRank23Max(String bossId) {
        return config.getInt("bosses." + bossId + ".rewards.rank-2-3.max", 4);
    }

    public String getRank23ItemId(String bossId) {
        return config.getString("bosses." + bossId + ".rewards.rank-2-3.item-id", "super_iron");
    }

    public int getRankOtherAmount(String bossId) {
        return config.getInt("bosses." + bossId + ".rewards.rank-other.amount", 1);
    }

    public String getRankOtherItemId(String bossId) {
        return config.getString("bosses." + bossId + ".rewards.rank-other.item-id", "super_iron");
    }

    // Enrage
    public boolean isEnrageEnabled(String bossId) {
        return config.getBoolean("bosses." + bossId + ".enrage.enabled", true);
    }

    public double getEnrageHealthPercent(String bossId) {
        return config.getDouble("bosses." + bossId + ".enrage.health-percent", 25);
    }

    public String getEnrageBroadcast(String bossId) {
        return config.getString("bosses." + bossId + ".enrage.broadcast", "§cBoss enraged!");
    }

    public double getFollowRange(String bossId) {
        return config.getDouble("bosses." + bossId + ".follow-range", 16);
    }

    public double getAttackRange(String bossId) {
        return config.getDouble("bosses." + bossId + ".attack-range", 3);
    }

    public boolean isRangedAttackEnabled(String bossId) {
        return config.getBoolean("bosses." + bossId + ".ranged-attack.enabled", false);
    }

    public int getRangedCooldown(String bossId) {
        return config.getInt("bosses." + bossId + ".ranged-attack.cooldown", 80);
    }

    public double getRangedRange(String bossId) {
        return config.getDouble("bosses." + bossId + ".ranged-attack.range", 20);
    }

    public double getRangedDamage(String bossId) {
        return config.getDouble("bosses." + bossId + ".ranged-attack.damage", 10);
    }

}
