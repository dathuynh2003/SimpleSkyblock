package com.dathuynh.simpleskyblock.utils;

import com.dathuynh.simpleskyblock.Main;
import com.dathuynh.simpleskyblock.models.CustomItem;
import com.dathuynh.simpleskyblock.models.NPCData;
import com.dathuynh.simpleskyblock.models.TradeData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigLoader {

    private Main plugin;
    private Map<String, CustomItem> customItems;
    private Map<String, NPCData> npcDataMap;

    public ConfigLoader(Main plugin) {
        this.plugin = plugin;
        this.customItems = new HashMap<>();
        this.npcDataMap = new HashMap<>();
        loadConfigs();
    }

    private void loadConfigs() {
        // Tạo files mặc định nếu chưa có
        saveDefaultConfig("items.yml");
        saveDefaultConfig("npcs_config.yml");

        // Load custom items trước
        loadCustomItems();

        // Load NPCs
        loadNPCs();
    }

    private void saveDefaultConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    private void loadCustomItems() {
        File file = new File(plugin.getDataFolder(), "items.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) return;

        for (String itemId : itemsSection.getKeys(false)) {
            CustomItem item = parseCustomItem(itemId, itemsSection.getConfigurationSection(itemId));
            customItems.put(itemId, item);
        }

        plugin.getLogger().info("Đã load " + customItems.size() + " custom items");
    }

    private CustomItem parseCustomItem(String id, ConfigurationSection section) {
        CustomItem item = new CustomItem(id);

        // Basic properties
        item.setMaterial(Material.valueOf(section.getString("material", "DIAMOND_SWORD")));
        item.setDisplayName(section.getString("display-name", "§fCustom Item"));
        item.setLore(section.getStringList("lore"));
        item.setUnbreakable(section.getBoolean("unbreakable", false));

        // ← THÊM CUSTOM MODEL DATA NGAY ĐÂY
        item.setCustomModelData(section.getInt("custom-model-data", 0));

        // Enchantments
        ConfigurationSection enchants = section.getConfigurationSection("enchantments");
        if (enchants != null) {
            for (String enchantName : enchants.getKeys(false)) {
                try {
                    Enchantment enchant = Enchantment.getByName(enchantName.toUpperCase());
                    int level = enchants.getInt(enchantName);
                    if (enchant != null) {
                        item.addEnchantment(enchant, level);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid enchantment: " + enchantName);
                }
            }
        }

        // Custom attributes
        ConfigurationSection attributes = section.getConfigurationSection("attributes");
        if (attributes != null) {
            if (attributes.contains("damage")) {
                item.setDamage(attributes.getDouble("damage"));
            }
            if (attributes.contains("attack-speed")) {
                item.setAttackSpeed(attributes.getDouble("attack-speed"));
            }
            if (attributes.contains("armor")) {
                item.setArmor(attributes.getDouble("armor"));
            }
            if (attributes.contains("max-health")) {
                item.setMaxHealth(attributes.getDouble("max-health"));
            }
            if (attributes.contains("knockback-resistance")) {
                item.setKnockbackResistance(attributes.getDouble("knockback-resistance"));
            }
            if (attributes.contains("movement-speed")) {
                item.setMovementSpeed(attributes.getDouble("movement-speed"));
            }
        }

        return item;
    }

    private void loadNPCs() {
        File file = new File(plugin.getDataFolder(), "npcs_config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection npcsSection = config.getConfigurationSection("npcs");
        if (npcsSection == null) return;

        for (String npcId : npcsSection.getKeys(false)) {
            NPCData npcData = parseNPC(npcId, npcsSection.getConfigurationSection(npcId));
            npcDataMap.put(npcId, npcData);
        }

        plugin.getLogger().info("Đã load " + npcDataMap.size() + " NPC configs");
    }

    private NPCData parseNPC(String id, ConfigurationSection section) {
        String displayName = section.getString("display-name", "§eNPC");
        Villager.Profession profession = Villager.Profession.valueOf(section.getString("profession", "TOOLSMITH"));
        int level = section.getInt("level", 5);

        // Parse trades
        List<TradeData> trades = new ArrayList<>();
        ConfigurationSection tradesSection = section.getConfigurationSection("trades");
        if (tradesSection != null) {
            for (String tradeId : tradesSection.getKeys(false)) {
                TradeData trade = parseTrade(tradeId, tradesSection.getConfigurationSection(tradeId));
                trades.add(trade);
            }
        }

        return new NPCData(id, displayName, profession, level, trades);
    }

    private TradeData parseTrade(String id, ConfigurationSection section) {
        int guiSlot = section.getInt("gui-slot", 14);

        // Parse required items
        List<ItemStack> requiredItems = new ArrayList<>();
        List<String> requirementsList = section.getStringList("requirements");
        for (String req : requirementsList) {
            ItemStack item = parseItemString(req);
            if (item != null) {
                requiredItems.add(item);
            }
        }

        // Parse reward item
        String rewardString = section.getString("reward");
        ItemStack rewardItem = parseItemString(rewardString);

        return new TradeData(id, requiredItems, rewardItem, guiSlot);
    }

    private ItemStack parseItemString(String itemString) {
        // Format: "MATERIAL:amount" hoặc "custom:item_id"
        String[] parts = itemString.split(":");

        if (parts[0].equalsIgnoreCase("custom")) {
            // Custom item
            CustomItem customItem = customItems.get(parts[1]);
            if (customItem != null) {
                return buildItemFromCustom(customItem);
            }
        } else {
            // Vanilla item
            try {
                Material material = Material.valueOf(parts[0].toUpperCase());
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                return new ItemStack(material, amount);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid item: " + itemString);
            }
        }

        return null;
    }

    private ItemStack buildItemFromCustom(CustomItem customItem) {
        ItemBuilder builder = new ItemBuilder(customItem.getMaterial()).setName(customItem.getDisplayName()).setUnbreakable(customItem.isUnbreakable());

        if (customItem.getLore() != null) {
            builder.setLore(customItem.getLore());
        }

        // Enchantments
        for (Map.Entry<Enchantment, Integer> entry : customItem.getEnchantments().entrySet()) {
            builder.addEnchant(entry.getKey(), entry.getValue());
        }

        // Attributes
        if (customItem.getDamage() != null) {
            builder.setDamage(customItem.getDamage());
        }
        if (customItem.getAttackSpeed() != null) {
            builder.setAttackSpeed(customItem.getAttackSpeed());
        }
        if (customItem.getArmor() != null) {
            builder.setArmor(customItem.getArmor());
        }
        if (customItem.getMaxHealth() != null) {
            builder.setMaxHealth(customItem.getMaxHealth());
        }
        if (customItem.getKnockbackResistance() != null) {
            builder.setKnockbackResistance(customItem.getKnockbackResistance());
        }
        if (customItem.getMovementSpeed() != null) {
            builder.setMovementSpeed(customItem.getMovementSpeed());
        }

        if (customItem.getCustomModelData() > 0) {
            plugin.getLogger().info("✅ ĐANG SET CustomModelData=" + customItem.getCustomModelData() + " cho " + customItem.getId());
            builder.setCustomModelData(customItem.getCustomModelData());
        } else {
            plugin.getLogger().warning("❌ CustomModelData = 0 cho " + customItem.getId());
        }

        builder.hideAllFlags();

        ItemStack result = builder.build();

        // KIỂM TRA SAU KHI BUILD
        if (result.hasItemMeta() && result.getItemMeta().hasCustomModelData()) {
            plugin.getLogger().info("✅✅ Item " + customItem.getId() + " ĐÃ CÓ CustomModelData=" + result.getItemMeta().getCustomModelData());
        } else {
            plugin.getLogger().warning("❌❌ Item " + customItem.getId() + " KHÔNG CÓ CustomModelData sau khi build!");
        }

        return result;
    }

    // Getters
    public CustomItem getCustomItem(String id) {
        return customItems.get(id);
    }

    public NPCData getNPCData(String id) {
        return npcDataMap.get(id);
    }

    public Map<String, NPCData> getAllNPCData() {
        return npcDataMap;
    }
}
