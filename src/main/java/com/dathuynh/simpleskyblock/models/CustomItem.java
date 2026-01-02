package com.dathuynh.simpleskyblock.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomItem {

    private String id;
    private Material material;
    private String displayName;
    private List<String> lore;
    private Map<Enchantment, Integer> enchantments;
    private boolean unbreakable;
    private int customModelData = 0;

    // Custom attributes
    private Double damage;
    private Double attackSpeed;
    private Double armor;
    private Double maxHealth;
    private Double knockbackResistance;
    private Double movementSpeed;

    public CustomItem(String id) {
        this.id = id;
        this.enchantments = new HashMap<>();
    }

    // Builder pattern methods
    public CustomItem setMaterial(Material material) {
        this.material = material;
        return this;
    }

    public CustomItem setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public CustomItem setLore(List<String> lore) {
        this.lore = lore;
        return this;
    }

    public CustomItem addEnchantment(Enchantment enchant, int level) {
        this.enchantments.put(enchant, level);
        return this;
    }

    public CustomItem setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    public CustomItem setDamage(double damage) {
        this.damage = damage;
        return this;
    }

    public CustomItem setAttackSpeed(double attackSpeed) {
        this.attackSpeed = attackSpeed;
        return this;
    }

    public CustomItem setArmor(double armor) {
        this.armor = armor;
        return this;
    }

    public CustomItem setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
        return this;
    }

    public CustomItem setKnockbackResistance(double knockbackResistance) {
        this.knockbackResistance = knockbackResistance;
        return this;
    }

    public CustomItem setMovementSpeed(double movementSpeed) {
        this.movementSpeed = movementSpeed;
        return this;
    }

    // Getters
    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public Double getDamage() {
        return damage;
    }

    public Double getAttackSpeed() {
        return attackSpeed;
    }

    public Double getArmor() {
        return armor;
    }

    public Double getMaxHealth() {
        return maxHealth;
    }

    public Double getKnockbackResistance() {
        return knockbackResistance;
    }

    public Double getMovementSpeed() {
        return movementSpeed;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }
}
