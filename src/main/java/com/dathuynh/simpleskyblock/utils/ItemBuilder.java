package com.dathuynh.simpleskyblock.utils;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private ItemStack item;
    private ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder setName(String name) {
        meta.setDisplayName(name);
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        List<String> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(line);
        }
        meta.setLore(loreList);
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchant, int level) {
        meta.addEnchant(enchant, level, true);
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder hideAllFlags() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    // ========== CUSTOM ATTRIBUTES ==========

    /**
     * Thêm damage cho vũ khí
     *
     * @param damage Sát thương (VD: 100 = 50 trái tim)
     */
    public ItemBuilder setDamage(double damage) {
        AttributeModifier modifier = new AttributeModifier(
                UUID.randomUUID(),
                "generic.attackDamage",
                damage,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        );
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, modifier);
        return this;
    }

    /**
     * Thêm tốc độ đánh
     *
     * @param speed Tốc độ (âm = chậm, dương = nhanh)
     */
    public ItemBuilder setAttackSpeed(double speed) {
        AttributeModifier modifier = new AttributeModifier(
                UUID.randomUUID(),
                "generic.attackSpeed",
                speed,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlot.HAND
        );
        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, modifier);
        return this;
    }

    /**
     * Thêm armor cho giáp
     *
     * @param armor Giáp (VD: 20 = full diamond)
     */
    public ItemBuilder setArmor(double armor, EquipmentSlot slot) {
        AttributeModifier modifier = new AttributeModifier(
                UUID.randomUUID(),
                "generic.armor",
                armor,
                AttributeModifier.Operation.ADD_NUMBER,
                slot
        );
        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, modifier);
        return this;
    }

    /**
     * Thêm tốc độ di chuyển
     *
     * @param speed Tốc độ (0.1 = +10%, 1.0 = +100%)
     */
    public ItemBuilder setMovementSpeed(double speed, EquipmentSlot slot) {
        AttributeModifier modifier = new AttributeModifier(
                UUID.randomUUID(),
                "generic.movementSpeed",
                speed,
                AttributeModifier.Operation.ADD_SCALAR, // Nhân phần trăm
                slot
        );
        meta.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, modifier);
        return this;
    }

    /**
     * Thêm knockback resistance (chống văng)
     *
     * @param resistance 0.0 - 1.0 (1.0 = không bị văng)
     */
    public ItemBuilder setKnockbackResistance(double resistance, EquipmentSlot slot) {
        AttributeModifier modifier = new AttributeModifier(
                UUID.randomUUID(),
                "generic.knockbackResistance",
                resistance,
                AttributeModifier.Operation.ADD_NUMBER,
                slot  // ← DÙNG PARAMETER!
        );
        meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE, modifier);
        return this;
    }

    /**
     * Thêm max health (máu)
     *
     * @param health Máu thêm (VD: 20 = +10 trái tim)
     */
    public ItemBuilder setMaxHealth(double health, EquipmentSlot slot) {
        AttributeModifier modifier = new AttributeModifier(
                UUID.randomUUID(),
                "generic.maxHealth",
                health,
                AttributeModifier.Operation.ADD_NUMBER,
                slot
        );
        meta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, modifier);
        return this;
    }

    /**
     * Custom attribute modifier
     */
    public ItemBuilder addAttribute(Attribute attribute, double amount,
                                    AttributeModifier.Operation operation,
                                    EquipmentSlot slot) {
        AttributeModifier modifier = new AttributeModifier(
                UUID.randomUUID(),
                attribute.getKey().getKey(),
                amount,
                operation,
                slot
        );
        meta.addAttributeModifier(attribute, modifier);
        return this;
    }

    public ItemBuilder setCustomModelData(int data) {
        if (data > 0) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }


}
