package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.managers.IslandManager;
import com.dathuynh.simpleskyblock.managers.SpawnManager;
import com.dathuynh.simpleskyblock.models.Island;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class RespawnListener implements Listener {

    private IslandManager islandManager;
    private SpawnManager spawnManager;

    // ✅ THÊM SpawnManager vào constructor
    public RespawnListener(IslandManager islandManager, SpawnManager spawnManager) {
        this.islandManager = islandManager;
        this.spawnManager = spawnManager;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Island island = islandManager.getIsland(player.getUniqueId());

        event.setRespawnLocation(spawnManager.getSpawnLocation());
        player.sendMessage("§7Bạn đã hồi sinh tại §bLobby§7!");
        if (island != null && island.getLocation() != null) {
            player.sendMessage("§7Sử dụng §e/is home §7để về đảo!");
        } else {
            player.sendMessage("§7Sử dụng §e/is create §7để tạo đảo riêng!");
        }
    }
}
