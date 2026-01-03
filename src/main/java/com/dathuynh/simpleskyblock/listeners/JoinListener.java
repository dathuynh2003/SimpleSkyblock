package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.managers.BossManager;
import com.dathuynh.simpleskyblock.managers.SpawnManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class JoinListener implements Listener {

    private SpawnManager spawnManager;
    private BossManager bossManager;
    private JavaPlugin plugin;

    public JoinListener(SpawnManager spawnManager, JavaPlugin plugin, BossManager bossManager) {
        this.spawnManager = spawnManager;
        this.plugin = plugin;
        this.bossManager = bossManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Chỉ teleport người chơi mới lần đầu join
        if (!player.hasPlayedBefore()) {
            // Delay 10 ticks (0.5 giây) để player load hoàn toàn
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.teleport(spawnManager.getSpawnLocation());
                player.sendMessage("§6═══════════════════════════════");
                player.sendMessage("§e✦ §aChào mừng đến DatHuynh Skyblock Server! §e✦");
                player.sendMessage("§7Bạn đang ở §bLobby §7- Trung tâm server");
                player.sendMessage("§7Sử dụng §e/is create §7để tạo đảo riêng!");
                player.sendMessage("§7Sử dụng §e/spawn §7để quay về lobby!");
                player.sendMessage("§6═══════════════════════════════");
            }, 10L);

            bossManager.addPlayerToBossBar(event.getPlayer());
        }
    }
}
