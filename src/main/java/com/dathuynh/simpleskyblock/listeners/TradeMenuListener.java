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

        // Check nếu là NPC menu
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

        // Tìm trade tương ứng với slot
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

        // Check xem player có đủ items không
        for (ItemStack required : requirements) {
            if (!hasItem(player, required)) {
                String itemName = required.hasItemMeta() && required.getItemMeta().hasDisplayName()
                        ? required.getItemMeta().getDisplayName()
                        : required.getType().name();
                player.sendMessage("§cBạn không đủ " + required.getAmount() + "x " + itemName + "!");
                player.closeInventory();
                return;
            }
        }

        // Remove required items
        for (ItemStack required : requirements) {
            removeItem(player, required);
        }

        // Give reward
        ItemStack reward = trade.getRewardItem().clone();
        player.getInventory().addItem(reward);

        String rewardName = reward.hasItemMeta() && reward.getItemMeta().hasDisplayName()
                ? reward.getItemMeta().getDisplayName()
                : reward.getType().name();

        player.sendMessage("§a§l✔ §aĐổi thành công! Bạn nhận được " + rewardName + "§a!");
        player.closeInventory();
    }

    private boolean hasItem(Player player, ItemStack required) {
        // Nếu là custom item (có display name)
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
            // Vanilla item
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == required.getType()) {
                    // Chỉ đếm items không có custom name
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
            // Custom item
            String targetName = required.getItemMeta().getDisplayName();

            for (ItemStack item : player.getInventory().getContents()) {
                if (remaining <= 0) break;

                if (item != null && item.getType() == required.getType()) {
                    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                        if (item.getItemMeta().getDisplayName().equals(targetName)) {
                            int itemAmount = item.getAmount();
                            if (itemAmount <= remaining) {
                                remaining -= itemAmount;
                                player.getInventory().remove(item);
                            } else {
                                item.setAmount(itemAmount - remaining);
                                remaining = 0;
                            }
                        }
                    }
                }
            }
        } else {
            // Vanilla item
            for (ItemStack item : player.getInventory().getContents()) {
                if (remaining <= 0) break;

                if (item != null && item.getType() == required.getType()) {
                    if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
                        int itemAmount = item.getAmount();
                        if (itemAmount <= remaining) {
                            remaining -= itemAmount;
                            player.getInventory().remove(item);
                        } else {
                            item.setAmount(itemAmount - remaining);
                            remaining = 0;
                        }
                    }
                }
            }
        }
    }
}
