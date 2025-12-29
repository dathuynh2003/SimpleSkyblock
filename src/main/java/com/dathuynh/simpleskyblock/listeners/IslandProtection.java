package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.managers.IslandManager;
import com.dathuynh.simpleskyblock.managers.SpawnManager;
import com.dathuynh.simpleskyblock.models.Island;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class IslandProtection implements Listener {

    private IslandManager islandManager;
    private static final int ISLAND_RADIUS = 50;

    public IslandProtection(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location blockLoc = event.getBlock().getLocation();

        if (event.getBlock().getType() == Material.BARRIER) {
            if (!player.isOp()) { // ✅ Chỉ chặn non-OP
                event.setCancelled(true);
                player.sendMessage("§c⚠ Đây là ranh giới đảo, không thể phá!");
            }
            return;
        }

        // Admin bypass
        if (player.isOp()) {
            return;
        }

        // Lấy island của player
        Island playerIsland = islandManager.getIsland(player.getUniqueId());

        // Kiểm tra xem block có nằm trong island của player không
        if (playerIsland != null && isLocationInIsland(blockLoc, playerIsland.getLocation())) {
            // Player đang phá block trong island của mình - cho phép
            return;
        }

        // Kiểm tra xem block có nằm trong island của người khác không
        if (isInAnyIsland(blockLoc)) {
            event.setCancelled(true);
            player.sendMessage("§cBạn không thể phá block ở đảo của người khác!");
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

        // Lấy island của player
        Island playerIsland = islandManager.getIsland(player.getUniqueId());

        if (playerIsland == null) {
            // Player không có island
            event.setCancelled(true);
            player.sendMessage("§cBạn phải có đảo mới được đặt block! Dùng /is create");
            return;
        }

        Location islandCenter = playerIsland.getLocation();

        int dx = Math.abs(blockLoc.getBlockX() - islandCenter.getBlockX());
        int dz = Math.abs(blockLoc.getBlockZ() - islandCenter.getBlockZ());
        if (dx >= ISLAND_RADIUS || dz >= ISLAND_RADIUS) {
            event.setCancelled(true);
            player.sendMessage("§c⚠ Bạn đã chạm ranh giới đảo (100x100)!");
            return;
        }

        // Kiểm tra xem block có nằm trong island của player không
        if (!isLocationInIsland(blockLoc, playerIsland.getLocation())) {
            event.setCancelled(true);

            // Kiểm tra có phải đang đặt ở đảo người khác không
            if (isInAnyIsland(blockLoc)) {
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

        // Lấy island của player
        Island playerIsland = islandManager.getIsland(player.getUniqueId());

        if (playerIsland == null) {
            return; // Player chưa có island
        }

        Location islandCenter = playerIsland.getLocation();

        // Kiểm tra nếu player đang ở ngoài island của mình
        if (!isLocationInIsland(to, islandCenter)) {
            return; // Không cảnh báo nếu đã ra ngoài
        }

        // Cảnh báo khi gần biên giới (48-50 blocks)
        int dx = Math.abs(to.getBlockX() - islandCenter.getBlockX());
        int dz = Math.abs(to.getBlockZ() - islandCenter.getBlockZ());

        if (dx >= 50 || dz >= 50) {
            player.sendMessage("§c⚠ Bạn đang ở rìa đảo! Giới hạn: 100x100 blocks");
        }
    }

    // Helper method: kiểm tra location có trong island cụ thể không
    private boolean isLocationInIsland(Location loc, Location islandCenter) {
        if (!loc.getWorld().equals(islandCenter.getWorld())) {
            return false;
        }

        int dx = Math.abs(loc.getBlockX() - islandCenter.getBlockX());
        int dz = Math.abs(loc.getBlockZ() - islandCenter.getBlockZ());

        return dx < ISLAND_RADIUS && dz < ISLAND_RADIUS;
    }

    // Helper method: kiểm tra location có trong bất kỳ island nào không
    private boolean isInAnyIsland(Location loc) {
        // Duyệt qua tất cả islands để kiểm tra
        for (Island island : getAllIslands()) {
            if (isLocationInIsland(loc, island.getLocation())) {
                return true;
            }
        }
        return false;
    }

    // Lấy tất cả islands từ manager
    private Iterable<Island> getAllIslands() {
        return islandManager.getAllIslands();
    }
}
