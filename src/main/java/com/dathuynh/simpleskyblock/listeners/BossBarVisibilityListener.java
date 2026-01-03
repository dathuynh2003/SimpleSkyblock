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

    // Cache trạng thái để tránh check lại không cần thiết
    private Map<UUID, Boolean> playerArenaStatus = new HashMap<>();

    // ✅ FIX: Debounce để tránh double-trigger
    private Map<UUID, Long> lastUpdateTime = new HashMap<>();
    private static final long UPDATE_COOLDOWN = 100; // 100ms cooldown

    public BossBarVisibilityListener(BossManager bossManager, ArenaManager arenaManager) {
        this.bossManager = bossManager;
        this.arenaManager = arenaManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Chỉ check khi player di chuyển BLOCK (bỏ qua rotation)
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return; // Chỉ xoay người, không di chuyển
        }

        checkAndUpdateBossBar(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // ✅ FIX: Delay 1 tick để tránh race condition với PlayerMoveEvent
        Player player = event.getPlayer();
        Location to = event.getTo();

        org.bukkit.Bukkit.getScheduler().runTaskLater(
                bossManager.getPlugin(),
                () -> checkAndUpdateBossBar(player, to),
                1L // 1 tick delay
        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay 5 ticks để đảm bảo player đã fully loaded
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
        // Return ngay nếu boss không sống
        if (!bossManager.isBossAlive()) {
            playerArenaStatus.remove(player.getUniqueId());
            lastUpdateTime.remove(player.getUniqueId());
            return;
        }

        UUID playerId = player.getUniqueId();

        // ✅ FIX: Debounce - Tránh update quá nhanh (double trigger)
        long now = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(playerId);
        if (lastUpdate != null && (now - lastUpdate) < UPDATE_COOLDOWN) {
            return; // Bỏ qua update này (quá nhanh)
        }

        boolean nowInArena = arenaManager.isInArena(location);
        Boolean wasInArena = playerArenaStatus.get(playerId);

        // Chỉ update khi trạng thái THỰC SỰ thay đổi
        if (wasInArena == null || wasInArena != nowInArena) {
            // Trạng thái thay đổi → update BossBar
            bossManager.updateBossBarVisibility(player);
            playerArenaStatus.put(playerId, nowInArena);
            lastUpdateTime.put(playerId, now);

            String status = nowInArena ? "entered" : "left";
            bossManager.getPlugin().getLogger().info("[BossBar] " + player.getName() + " " + status + " arena");
        }
    }

    /**
     * Cleanup cache khi player rời server
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        playerArenaStatus.remove(playerId);
        lastUpdateTime.remove(playerId);
    }

    /**
     * Clear cache (gọi khi boss chết/respawn)
     */
    public void clearCache() {
        playerArenaStatus.clear();
        lastUpdateTime.clear();
        bossManager.getPlugin().getLogger().info("[BossBar] Cache cleared");
    }

    /**
     * Manually set cache (called when boss spawns with players already in arena)
     */
    public void setCacheStatus(UUID playerId, boolean inArena) {
        playerArenaStatus.put(playerId, inArena);
    }
}
