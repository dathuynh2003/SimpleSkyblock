package com.dathuynh.simpleskyblock.managers;

import com.dathuynh.simpleskyblock.Main;
import com.dathuynh.simpleskyblock.models.CustomItem;
import com.dathuynh.simpleskyblock.utils.ConfigLoader;
import com.dathuynh.simpleskyblock.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;

public class ItemManager {

    private Main plugin;
    private ConfigLoader configLoader;

    public ItemManager(Main plugin) {
        this.plugin = plugin;
        this.configLoader = plugin.getConfigLoader();
    }

    public ItemStack getItem(String itemId) {
        CustomItem customItem = configLoader.getCustomItem(itemId);

        if (customItem == null) {
            plugin.getLogger().warning("Item '" + itemId + "' not found in items.yml!");
            return null;
        }

        return buildItemFromCustom(customItem);
    }

    private ItemStack buildItemFromCustom(CustomItem customItem) {
        ItemBuilder builder = new ItemBuilder(customItem.getMaterial())
                .setName(customItem.getDisplayName())
                .setUnbreakable(customItem.isUnbreakable());

        if (customItem.getLore() != null && !customItem.getLore().isEmpty()) {
            builder.setLore(customItem.getLore());
        }

        for (Map.Entry<Enchantment, Integer> entry : customItem.getEnchantments().entrySet()) {
            builder.addEnchant(entry.getKey(), entry.getValue());
        }

        if (customItem.getCustomModelData() > 0) {
            builder.setCustomModelData(customItem.getCustomModelData());
        }

        // ✅ TỰ ĐỘNG DETECT SLOT TỪ MATERIAL
        EquipmentSlot slot = getEquipmentSlot(customItem.getMaterial());

        // Attributes với dynamic slot
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
            builder.setMovementSpeed(customItem.getMovementSpeed());
        }

        builder.hideAllFlags();

        return builder.build();
    }

    private EquipmentSlot getEquipmentSlot(Material material) {
        String name = material.name().toUpperCase();

        // Helmets
        if (name.contains("HELMET") || name.contains("HEAD") ||
                name.equals("SKULL") || name.equals("PLAYER_HEAD") ||
                name.contains("CARVED_PUMPKIN") || name.contains("TURTLE_HELMET")) {
            return EquipmentSlot.HEAD;
        }

        // Chestplates
        if (name.contains("CHESTPLATE") || name.contains("ELYTRA")) {
            return EquipmentSlot.CHEST;
        }

        // Leggings
        if (name.contains("LEGGINGS") || name.contains("PANTS")) {
            return EquipmentSlot.LEGS;
        }

        // Boots
        if (name.contains("BOOTS") || name.contains("SHOES")) {
            return EquipmentSlot.FEET;
        }

        // Weapons & Tools (Main Hand)
        if (name.contains("SWORD") || name.contains("AXE") ||
                name.contains("PICKAXE") || name.contains("SHOVEL") ||
                name.contains("HOE") || name.contains("BOW") ||
                name.contains("CROSSBOW") || name.contains("TRIDENT") ||
                name.contains("FISHING_ROD") || name.contains("SHEARS")) {
            return EquipmentSlot.HAND;
        }

        // Default: HAND (for shields, totems, etc.)
        return EquipmentSlot.OFF_HAND;
    }

    public boolean hasItem(String itemId) {
        return configLoader.getCustomItem(itemId) != null;
    }

    public void reload() {
        plugin.getLogger().info("ItemManager reloaded!");
    }

    public Set<String> getAllItemIds() {
        return configLoader.getAllCustomItems().keySet();
    }
}
