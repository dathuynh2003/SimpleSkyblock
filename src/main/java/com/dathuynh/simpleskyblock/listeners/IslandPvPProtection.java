package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.managers.IslandManager;
import com.dathuynh.simpleskyblock.models.Island;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class IslandPvPProtection implements Listener {

    private IslandManager islandManager;

    public IslandPvPProtection(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // Check cả attacker và victim đều là player
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Admin bypass
        if (attacker.isOp() && victim.isOp()) {
            return;
        }

        // Check victim có đang ở trong island không
        Island victimIsland = getIslandAtLocation(victim.getLocation());

        if (victimIsland != null) {
            // Victim đang ở trong một island → Cancel PvP
            event.setCancelled(true);
            attacker.sendMessage("§cKhông thể PvP trong island!");
            return;
        }

        // Check attacker có đang ở trong island không
        Island attackerIsland = getIslandAtLocation(attacker.getLocation());

        if (attackerIsland != null) {
            // Attacker đang ở trong island → Cancel PvP
            event.setCancelled(true);
            attacker.sendMessage("§cKhông thể PvP trong island!");
        }
    }

    // Helper: Tìm island tại location
    private Island getIslandAtLocation(org.bukkit.Location loc) {
        for (Island island : islandManager.getAllIslands()) {
            if (islandManager.isLocationInIsland(loc, island.getLocation())) {
                return island;
            }
        }
        return null;
    }
}
