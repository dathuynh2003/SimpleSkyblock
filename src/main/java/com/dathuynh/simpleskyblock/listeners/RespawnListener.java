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

        //Nếu có island → luôn respawn tại island (bất kể chết ở đâu)
        if (island != null && island.getLocation() != null) {
            event.setRespawnLocation(island.getLocation());
            player.sendMessage("§aĐã hồi sinh tại đảo của bạn!");
        }
        //Nếu không có island → respawn tại lobby
        else {
            event.setRespawnLocation(spawnManager.getSpawnLocation());
            player.sendMessage("§eĐã hồi sinh tại Lobby!");
            player.sendMessage("§7Sử dụng §e/is create §7để tạo đảo riêng!");
        }
    }
}
