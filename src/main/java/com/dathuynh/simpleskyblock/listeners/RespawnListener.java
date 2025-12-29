package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.managers.IslandManager;
import com.dathuynh.simpleskyblock.models.Island;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class RespawnListener implements Listener {

    private IslandManager islandManager;

    public RespawnListener(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Lấy đảo của player
        Island island = islandManager.getIsland(event.getPlayer().getUniqueId());

        if (island != null) {
            // Lấy death location
            Location deathLoc = event.getPlayer().getLastDeathLocation();

            // Nếu chết trong khu vực đảo của mình -> respawn tại đảo
            if (deathLoc != null && islandManager.isLocationInIsland(deathLoc, island.getLocation())) {
                event.setRespawnLocation(island.getLocation());
            }
            // Nếu chết ngoài đảo -> respawn mặc định (spawn lobby)
        }
    }
}
