package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.models.NPCData;
import com.dathuynh.simpleskyblock.models.TradeData;
import com.dathuynh.simpleskyblock.utils.ConfigLoader;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class TradeMenuListener implements Listener {

    private ConfigLoader configLoader;

    public TradeMenuListener(ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        NPCData npcData = findNPCByTitle(title);
        if (npcData == null) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        for (TradeData trade : npcData.getTrades()) {
            if (trade.getGuiSlot() == slot) {
                performTrade(player, trade);
                return;
            }
        }
    }

    private NPCData findNPCByTitle(String title) {
        for (NPCData npcData : configLoader.getAllNPCData().values()) {
            if (npcData.getDisplayName().equals(title)) {
                return npcData;
            }
        }
        return null;
    }

    private void performTrade(Player player, TradeData trade) {
        List<ItemStack> requirements = trade.getRequiredItems();

        for (ItemStack required : requirements) {
            if (!hasItem(player, required)) {
                String itemName = required.hasItemMeta() && required.getItemMeta().hasDisplayName()
                        ? required.getItemMeta().getDisplayName()
                        : required.getType().name();
                player.sendMessage("§cBạn không đủ " + required.getAmount() + "x " + itemName + "!");
                return;
            }
        }

        for (ItemStack required : requirements) {
            removeItem(player, required);
        }

        ItemStack reward = trade.getRewardItem().clone();
        player.getInventory().addItem(reward);

        String rewardName = reward.hasItemMeta() && reward.getItemMeta().hasDisplayName()
                ? reward.getItemMeta().getDisplayName()
                : reward.getType().name();

        player.sendMessage("§aThành công! Bạn nhận được " + rewardName + "§a!");
    }

    private boolean hasItem(Player player, ItemStack required) {
        if (required.hasItemMeta() && required.getItemMeta().hasDisplayName()) {
            String targetName = required.getItemMeta().getDisplayName();
            int count = 0;

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == required.getType()) {
                    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                        if (item.getItemMeta().getDisplayName().equals(targetName)) {
                            count += item.getAmount();
                        }
                    }
                }
            }

            return count >= required.getAmount();
        } else {
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == required.getType()) {
                    if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
                        count += item.getAmount();
                    }
                }
            }
            return count >= required.getAmount();
        }
    }

    private void removeItem(Player player, ItemStack required) {
        int remaining = required.getAmount();

        if (required.hasItemMeta() && required.getItemMeta().hasDisplayName()) {
            String targetName = required.getItemMeta().getDisplayName();

            for (int i = 0; i < player.getInventory().getSize(); i++) {
                if (remaining <= 0) break;

                ItemStack item = player.getInventory().getItem(i);
                if (item == null || item.getType() != required.getType()) continue;
                if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) continue;
                if (!item.getItemMeta().getDisplayName().equals(targetName)) continue;

                int itemAmount = item.getAmount();

                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        } else {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                if (remaining <= 0) break;

                ItemStack item = player.getInventory().getItem(i);
                if (item == null || item.getType() != required.getType()) continue;
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) continue;

                int itemAmount = item.getAmount();

                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }
    }
}
