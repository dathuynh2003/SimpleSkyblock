package com.dathuynh.simpleskyblock.models;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TradeData {

    private String id;
    private List<ItemStack> requiredItems; // Items cần để trade
    private ItemStack rewardItem; // Item nhận được
    private int guiSlot; // Vị trí trong GUI

    public TradeData(String id, List<ItemStack> requiredItems, ItemStack rewardItem, int guiSlot) {
        this.id = id;
        this.requiredItems = requiredItems;
        this.rewardItem = rewardItem;
        this.guiSlot = guiSlot;
    }

    // Getters
    public String getId() {
        return id;
    }

    public List<ItemStack> getRequiredItems() {
        return requiredItems;
    }

    public ItemStack getRewardItem() {
        return rewardItem;
    }

    public int getGuiSlot() {
        return guiSlot;
    }
}
