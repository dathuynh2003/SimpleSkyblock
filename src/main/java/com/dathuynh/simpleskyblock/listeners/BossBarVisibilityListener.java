package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.managers.ArenaManager;
import com.dathuynh.simpleskyblock.managers.BossManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarVisibilityListener implements Listener {

    private BossManager bossManager;
    private ArenaManager arenaManager;

    private Map<UUID, Boolean> playerArenaStatus = new HashMap<>();
    private Map<UUID, Long> lastUpdateTime = new HashMap<>();
    private static final long UPDATE_COOLDOWN = 100;

    public BossBarVisibilityListener(BossManager bossManager, ArenaManager arenaManager) {
        this.bossManager = bossManager;
        this.arenaManager = arenaManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        checkAndUpdateBossBar(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();

        org.bukkit.Bukkit.getScheduler().runTaskLater(
                bossManager.getPlugin(),
                () -> checkAndUpdateBossBar(player, to),
                1L
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        org.bukkit.Bukkit.getScheduler().runTaskLater(
                bossManager.getPlugin(),
                () -> {
                    if (bossManager.isBossAlive()) {
                        checkAndUpdateBossBar(player, player.getLocation());
                    }
                },
                5L
        );
    }

    private void checkAndUpdateBossBar(Player player, Location location) {
        if (!bossManager.isBossAlive()) {
            playerArenaStatus.remove(player.getUniqueId());
            lastUpdateTime.remove(player.getUniqueId());
            return;
        }

        UUID playerId = player.getUniqueId();

        long now = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(playerId);
        if (lastUpdate != null && (now - lastUpdate) < UPDATE_COOLDOWN) {
            return;
        }

        boolean nowInArena = arenaManager.isInArena(location);
        Boolean wasInArena = playerArenaStatus.get(playerId);

        if (wasInArena == null || wasInArena != nowInArena) {
            bossManager.updateBossBarVisibility(player);
            playerArenaStatus.put(playerId, nowInArena);
            lastUpdateTime.put(playerId, now);
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        playerArenaStatus.remove(playerId);
        lastUpdateTime.remove(playerId);
    }

    public void clearCache() {
        playerArenaStatus.clear();
        lastUpdateTime.clear();
    }

    public void setCacheStatus(UUID playerId, boolean inArena) {
        playerArenaStatus.put(playerId, inArena);
    }
}
