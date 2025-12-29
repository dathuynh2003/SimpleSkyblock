package com.dathuynh.simpleskyblock.utils;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.*;

public class KitConfig {

    private JavaPlugin plugin;
    private Map<String, Kit> kits;

    public KitConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.kits = new HashMap<>();
        loadKits();
    }

    public static class Kit {
        private String id;
        private String displayName;
        private String description;
        private UsageType usageType;
        private long cooldownHours;
        private boolean requireIsland;
        private List<ItemStack> items;

        public Kit(String id, String displayName, String description,
                   UsageType usageType, long cooldownHours,
                   boolean requireIsland, List<ItemStack> items) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
            this.usageType = usageType;
            this.cooldownHours = cooldownHours;
            this.requireIsland = requireIsland;
            this.items = items;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public UsageType getUsageType() {
            return usageType;
        }

        public long getCooldownMs() {
            return cooldownHours * 60 * 60 * 1000L;
        }

        public boolean requiresIsland() {
            return requireIsland;
        }

        public List<ItemStack> getItems() {
            return items;
        }
    }

    public enum UsageType {
        ONCE,      // Chỉ 1 lần
        COOLDOWN   // Có cooldown
    }

    private void loadKits() {
        // Copy default config nếu chưa có
        File configFile = new File(plugin.getDataFolder(), "kits_config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("kits_config.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (!config.contains("kits")) {
            plugin.getLogger().warning("Không tìm thấy kits config!");
            return;
        }

        ConfigurationSection kitsSection = config.getConfigurationSection("kits");

        for (String kitId : kitsSection.getKeys(false)) {
            try {
                String path = "kits." + kitId;

                String displayName = config.getString(path + ".display-name", "§fKit");
                String description = config.getString(path + ".description", "");
                String usageTypeStr = config.getString(path + ".usage-type", "ONCE");
                UsageType usageType = UsageType.valueOf(usageTypeStr);
                long cooldownHours = config.getLong(path + ".cooldown-hours", 24);
                boolean requireIsland = config.getBoolean(path + ".require-island", true);

                // Parse items
                List<ItemStack> items = new ArrayList<>();
                List<String> itemStrings = config.getStringList(path + ".items");

                for (String itemStr : itemStrings) {
                    ItemStack item = parseItem(itemStr);
                    if (item != null) {
                        items.add(item);
                    }
                }

                Kit kit = new Kit(kitId, displayName, description,
                        usageType, cooldownHours, requireIsland, items);
                kits.put(kitId, kit);

                plugin.getLogger().info("Đã load kit: " + kitId);

            } catch (Exception e) {
                plugin.getLogger().warning("Lỗi khi load kit: " + kitId);
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Đã load " + kits.size() + " kits!");
    }

    private ItemStack parseItem(String itemStr) {
        String[] parts = itemStr.split(":");

        if (parts[0].equalsIgnoreCase("custom")) {
            // Custom item từ ConfigLoader
            // TODO: Tích hợp với ConfigLoader nếu cần
            return null;
        }

        try {
            Material material = Material.valueOf(parts[0].toUpperCase());
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

            ItemStack item = new ItemStack(material, amount);

            // TODO: Thêm display name và lore nếu cần
            // parts[2] = display name
            // parts[3] = lore

            return item;

        } catch (Exception e) {
            plugin.getLogger().warning("Không thể parse item: " + itemStr);
            return null;
        }
    }

    public Kit getKit(String kitId) {
        return kits.get(kitId.toLowerCase());
    }

    public Map<String, Kit> getAllKits() {
        return kits;
    }

    public void reload() {
        kits.clear();
        loadKits();
    }
}
