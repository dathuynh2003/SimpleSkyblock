package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.commands.IslandCommand;
import com.dathuynh.simpleskyblock.managers.SpawnManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class IslandProtection implements Listener {

    private IslandCommand islandCommand;

    public IslandProtection(IslandCommand islandCommand) {
        this.islandCommand = islandCommand;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location blockLoc = event.getBlock().getLocation();

        // Admin bypass
        if (player.isOp()) {
            return;
        }

        // Kiểm tra xem có phải đang ở island của mình không
        if (!islandCommand.isInOwnIsland(player, blockLoc)) {
            // Nếu ở island của người khác
            if (IslandCommand.isInAnyIsland(blockLoc, islandCommand.getIslands())) {
                event.setCancelled(true);
                player.sendMessage("§cBạn không thể phá block ở đảo của người khác!");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Location blockLoc = event.getBlock().getLocation();

        // Admin bypass
        if (player.isOp()) {
            return;
        }

        // Kiểm tra xem có phải đang ở island của mình không
        if (!islandCommand.isInOwnIsland(player, blockLoc)) {
            event.setCancelled(true);

            // Nếu ở island của người khác
            if (IslandCommand.isInAnyIsland(blockLoc, islandCommand.getIslands())) {
                player.sendMessage("§cBạn không thể đặt block ở đảo của người khác!");
            } else {
                player.sendMessage("§cBạn đã vượt quá giới hạn 100x100 của đảo!");
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();

        // Admin bypass
        if (player.isOp()) {
            return;
        }

        // Không check ở spawn
        if (SpawnManager.isInSpawnArea(to)) {
            return;
        }

        // Cảnh báo khi gần biên giới
        if (!islandCommand.isInOwnIsland(player, to)) {
            Location islandCenter = islandCommand.getIslands().get(player.getUniqueId());
            if (islandCenter != null) {
                int dx = Math.abs(to.getBlockX() - islandCenter.getBlockX());
                int dz = Math.abs(to.getBlockZ() - islandCenter.getBlockZ());

                // Nếu đang ở biên giới (48-50 blocks)
                if (dx >= 48 || dz >= 48) {
                    player.sendMessage("§c⚠ Bạn đang ở rìa đảo! Giới hạn: 100x100 blocks");
                }
            }
        }
    }
}
