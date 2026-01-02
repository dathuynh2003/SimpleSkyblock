package com.dathuynh.simpleskyblock.managers;

import com.dathuynh.simpleskyblock.Main;
import com.dathuynh.simpleskyblock.models.CustomItem;
import com.dathuynh.simpleskyblock.utils.ConfigLoader;
import com.dathuynh.simpleskyblock.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Manager để lấy items từ items.yml
 * Wrapper cho ConfigLoader
 */
public class ItemManager {

    private Main plugin;
    private ConfigLoader configLoader;

    public ItemManager(Main plugin) {
        this.plugin = plugin;
        this.configLoader = plugin.getConfigLoader(); // Lấy từ Main
    }

    /**
     * Get item from items.yml by ID
     *
     * @param itemId ID trong items.yml (e.g., "super_iron", "god_sword")
     * @return ItemStack hoặc null nếu không tìm thấy
     */
    public ItemStack getItem(String itemId) {
        CustomItem customItem = configLoader.getCustomItem(itemId);

        if (customItem == null) {
            plugin.getLogger().warning("Item '" + itemId + "' not found in items.yml!");
            return null;
        }

        return buildItemFromCustom(customItem);
    }

    /**
     * Build ItemStack from CustomItem model
     */
    private ItemStack buildItemFromCustom(CustomItem customItem) {
        ItemBuilder builder = new ItemBuilder(customItem.getMaterial())
                .setName(customItem.getDisplayName())
                .setUnbreakable(customItem.isUnbreakable());

        // Lore
        if (customItem.getLore() != null && !customItem.getLore().isEmpty()) {
            builder.setLore(customItem.getLore());
        }

        // Enchantments
        for (Map.Entry<Enchantment, Integer> entry : customItem.getEnchantments().entrySet()) {
            builder.addEnchant(entry.getKey(), entry.getValue());
        }

        // Custom Model Data
        if (customItem.getCustomModelData() > 0) {
            builder.setCustomModelData(customItem.getCustomModelData());
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

        // Hide all flags (enchant glint, attributes, etc.)
        builder.hideAllFlags();

        return builder.build();
    }

    /**
     * Check if item exists in config
     */
    public boolean hasItem(String itemId) {
        return configLoader.getCustomItem(itemId) != null;
    }

    /**
     * Reload items.yml
     */
    public void reload() {
        // ConfigLoader tự reload khi Main reload
        plugin.getLogger().info("ItemManager reloaded!");
    }
}
