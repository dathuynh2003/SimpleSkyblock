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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

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
        saveDefaultConfig("items.yml");
        saveDefaultConfig("npcs_config.yml");
        loadCustomItems();
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

        plugin.getLogger().info("Loaded " + customItems.size() + " custom items");
    }

    private CustomItem parseCustomItem(String id, ConfigurationSection section) {
        CustomItem item = new CustomItem(id);

        item.setMaterial(Material.valueOf(section.getString("material", "DIAMOND_SWORD")));
        item.setDisplayName(section.getString("display-name", "§fCustom Item"));
        item.setLore(section.getStringList("lore"));
        item.setUnbreakable(section.getBoolean("unbreakable", false));
        item.setCustomModelData(section.getInt("custom-model-data", 0));

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

        ConfigurationSection potionEffects = section.getConfigurationSection("potion-effects");
        if (potionEffects != null) {
            for (String effectName : potionEffects.getKeys(false)) {
                try {
                    PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
                    int level = potionEffects.getInt(effectName);
                    if (type != null) {
                        item.addPotionEffect(type, level - 1);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid potion effect: " + effectName);
                }
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

        plugin.getLogger().info("Loaded " + npcDataMap.size() + " NPC configs");
    }

    private NPCData parseNPC(String id, ConfigurationSection section) {
        String displayName = section.getString("display-name", "§eNPC");
        Villager.Profession profession = Villager.Profession.valueOf(section.getString("profession", "TOOLSMITH"));
        int level = section.getInt("level", 5);

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

        List<ItemStack> requiredItems = new ArrayList<>();
        List<String> requirementsList = section.getStringList("requirements");
        for (String req : requirementsList) {
            ItemStack item = parseItemString(req);
            if (item != null) {
                requiredItems.add(item);
            }
        }

        String rewardString = section.getString("reward");
        ItemStack rewardItem = parseItemString(rewardString);

        return new TradeData(id, requiredItems, rewardItem, guiSlot);
    }

    private ItemStack parseItemString(String itemString) {
        String[] parts = itemString.split(":");

        if (parts[0].equalsIgnoreCase("custom")) {
            CustomItem customItem = customItems.get(parts[1]);
            if (customItem != null) {
                ItemStack item = buildItemFromCustom(customItem);

                if (parts.length > 2) {
                    int amount = Integer.parseInt(parts[2]);
                    item.setAmount(amount);
                }

                return item;
            }
        } else {
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

    // ✅ FIX: Thêm slot parameter
    private ItemStack buildItemFromCustom(CustomItem customItem) {
        ItemBuilder builder = new ItemBuilder(customItem.getMaterial())
                .setName(customItem.getDisplayName())
                .setUnbreakable(customItem.isUnbreakable());

        if (customItem.getLore() != null) {
            builder.setLore(customItem.getLore());
        }

        for (Map.Entry<Enchantment, Integer> entry : customItem.getEnchantments().entrySet()) {
            builder.addEnchant(entry.getKey(), entry.getValue());
        }

        EquipmentSlot slot = getEquipmentSlot(customItem.getMaterial());

        if (customItem.getDamage() != null) {
            builder.setDamage(customItem.getDamage());
        }
        if (customItem.getAttackSpeed() != null) {
            builder.setAttackSpeed(customItem.getAttackSpeed());
        }
        if (customItem.getArmor() != null) {
            builder.setArmor(customItem.getArmor(), slot);
        }
        if (customItem.getMaxHealth() != null) {
            builder.setMaxHealth(customItem.getMaxHealth(), slot);
        }
        if (customItem.getKnockbackResistance() != null) {
            builder.setKnockbackResistance(customItem.getKnockbackResistance(), slot);
        }
        if (customItem.getMovementSpeed() != null) {
            builder.setMovementSpeed(customItem.getMovementSpeed(), slot);
        }

        if (customItem.getCustomModelData() > 0) {
            builder.setCustomModelData(customItem.getCustomModelData());
        }

        builder.hideAllFlags();

        return builder.build();
    }

    // ✅ THÊM METHOD DETECT SLOT
    private EquipmentSlot getEquipmentSlot(Material material) {
        String name = material.name().toUpperCase();

        if (name.contains("HELMET") || name.contains("HEAD") ||
                name.equals("PLAYER_HEAD") || name.equals("CARVED_PUMPKIN") ||
                name.equals("TURTLE_HELMET")) {
            return EquipmentSlot.HEAD;
        }

        if (name.contains("CHESTPLATE") || name.contains("ELYTRA")) {
            return EquipmentSlot.CHEST;
        }

        if (name.contains("LEGGINGS") || name.contains("PANTS")) {
            return EquipmentSlot.LEGS;
        }

        if (name.contains("BOOTS") || name.contains("SHOES")) {
            return EquipmentSlot.FEET;
        }

        if (name.equals("SHIELD") || name.equals("TOTEM_OF_UNDYING")) {
            return EquipmentSlot.OFF_HAND;
        }

        if (name.contains("SWORD") || name.contains("AXE") ||
                name.contains("PICKAXE") || name.contains("SHOVEL") ||
                name.contains("HOE") || name.contains("BOW") ||
                name.contains("CROSSBOW") || name.contains("TRIDENT") ||
                name.contains("FISHING_ROD") || name.contains("SHEARS")) {
            return EquipmentSlot.HAND;
        }

        return EquipmentSlot.OFF_HAND;
    }

    public CustomItem getCustomItem(String id) {
        return customItems.get(id);
    }

    public NPCData getNPCData(String id) {
        return npcDataMap.get(id);
    }

    public Map<String, NPCData> getAllNPCData() {
        return npcDataMap;
    }

    public Map<String, CustomItem> getAllCustomItems() {
        return customItems;
    }
}
