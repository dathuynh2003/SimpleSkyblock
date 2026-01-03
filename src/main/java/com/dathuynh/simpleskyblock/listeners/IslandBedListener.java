package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.managers.IslandManager;
import com.dathuynh.simpleskyblock.models.Island;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;

public class IslandBedListener implements Listener {

    private IslandManager islandManager;

    public IslandBedListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBedClick(PlayerInteractEvent event) {
        // Chỉ check right-click
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        // Check có phải giường không
        Material type = block.getType();
        if (!type.name().contains("BED")) return;

        Player player = event.getPlayer();
        Island island = islandManager.getIsland(player.getUniqueId());

        if (island == null) return;

        // Chỉ owner mới set được
        if (!island.isOwner(player.getUniqueId())) {
            return;
        }

        // Check có trong island của mình không
        if (!islandManager.isLocationInIsland(block.getLocation(), island.getLocation())) {
            return;
        }

        // Nếu player đang SHIFT + Right click → Set home
        if (player.isSneaking()) {
            // Get location trên giường (không bị stuck)
            org.bukkit.Location bedLoc = block.getLocation().add(0.5, 1, 0.5);
            bedLoc.setYaw(player.getLocation().getYaw());
            bedLoc.setPitch(player.getLocation().getPitch());

            boolean success = islandManager.setIslandHome(player.getUniqueId(), bedLoc);

            if (success) {
                event.setCancelled(true); // Không sleep
                player.sendMessage("§a✓ Đã đặt spawn point tại giường!");
                player.sendMessage("§7Hint: §eShift + Right Click §7để đặt spawn");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            }
        } else {
            // Normal click → hiện hint
            player.sendMessage("§7Hint: §eShift + Right Click §7để đặt spawn point tại giường!");
        }
    }
}
