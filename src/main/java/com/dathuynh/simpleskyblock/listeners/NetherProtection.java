package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.managers.NetherZoneManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class NetherProtection implements Listener {

    private NetherZoneManager netherZoneManager;

    public NetherProtection(NetherZoneManager netherZoneManager) {
        this.netherZoneManager = netherZoneManager;
    }

    /**
     * Prevent block break in nether
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Creative bypass
        if (player.getGameMode() == GameMode.CREATIVE) return;

        // Check if in nether world
        if (netherZoneManager.isInNetherZone(player.getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§c⚠ Không thể phá block trong khu vực Nether!");
        }
    }

    /**
     * Prevent block place in nether
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Creative bypass
        if (player.getGameMode() == GameMode.CREATIVE) return;

        // Check if in nether world
        if (netherZoneManager.isInNetherZone(player.getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§c⚠ Không thể đặt block trong khu vực Nether!");
        }
    }

    /**
     * Prevent PvP in nether
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Check if in nether world
        if (netherZoneManager.isInNetherZone(victim.getLocation())) {
            event.setCancelled(true);
            attacker.sendMessage("§c⚠ PvP không được phép trong khu vực Nether!");
        }
    }

    /**
     * Prevent item drop on death in nether
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Check if in nether world
        if (netherZoneManager.isInNetherZone(player.getLocation())) {
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

//            player.sendMessage("§aBạn đã giữ lại đồ và kinh nghiệm!");
        }
    }
}
