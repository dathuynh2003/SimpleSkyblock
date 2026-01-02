package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.managers.MiningZoneManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class MiningZoneProtection implements Listener {

    private MiningZoneManager miningZoneManager;

    public MiningZoneProtection(MiningZoneManager miningZoneManager) {
        this.miningZoneManager = miningZoneManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (!miningZoneManager.isInMiningZone(event.getBlock().getLocation())) {
            return;
        }

        // Chặn phá Bedrock
        if (event.getBlock().getType() == Material.BEDROCK) {
            if (!player.isOp()) {
                event.setCancelled(true);
                player.sendMessage("§cKhông thể phá bedrock!");
            }
        }

        // Cho phép phá các block khác
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        // Cho phép đặt block tự do trong khu mine
        if (!miningZoneManager.isInMiningZone(event.getBlock().getLocation())) {
            return;
        }

        // Chặn đặt TNT
        if (event.getBlock().getType() == Material.TNT ||
                event.getBlock().getType() == Material.TNT_MINECART) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cCấm sử dụng TNT trong khu mine!");
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getLocation() != null &&
                miningZoneManager.isInMiningZone(event.getLocation())) {
            event.setCancelled(true);

            if (event.getEntity() != null &&
                    event.getEntity().getType() == EntityType.PRIMED_TNT) {
                event.getLocation().getWorld().getNearbyPlayers(event.getLocation(), 10).forEach(player -> {
                    player.sendMessage("§c TNT bị vô hiệu hóa trong khu mine!");
                });
            }
        }
    }


    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (miningZoneManager.isInMiningZone(player.getLocation())) {
            // Cho phép rớt items khi chết trong khu mine
            event.setKeepInventory(false);
            event.setKeepLevel(false);

            player.sendMessage("§c☠ Bạn đã chết trong khu mine!");
        }
    }
}
