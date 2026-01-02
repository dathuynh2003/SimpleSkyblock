package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.Main;
import com.dathuynh.simpleskyblock.managers.AuthManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthListener implements Listener {

    private AuthManager authManager;
    private Main plugin;
    private Map<UUID, BukkitTask> loginTimeouts;

    public AuthListener(AuthManager authManager, Main plugin) {
        this.authManager = authManager;
        this.plugin = plugin;
        this.loginTimeouts = new HashMap<>();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

//        // Check auto-login (cùng IP trong 24h)
//        if (authManager.canAutoLogin(player)) {
//            authManager.login(player, ""); // Bypass password check
//            player.sendMessage("§aTự động đăng nhập thành công!");
//            return;
//        }

        // Nếu chưa đăng ký
        if (!authManager.isRegistered(uuid)) {
            player.sendMessage("§e═══════════════════════════════");
            player.sendMessage("§6Chào mừng đến server!");
            player.sendMessage("§7Vui lòng đăng ký tài khoản:");
            player.sendMessage("§e/register <password> <password>");
            player.sendMessage("§e═══════════════════════════════");
        }
        // Nếu đã đăng ký nhưng chưa login
        else {
            player.sendMessage("§e═══════════════════════════════");
            player.sendMessage("§6Vui lòng đăng nhập:");
            player.sendMessage("§e/login <password>");
            player.sendMessage("§e═══════════════════════════════");
        }

        // Set spectator mode cho đến khi login
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(plugin.getSpawnManager().getSpawnLocation());

        // Start timeout (60 giây)
        startLoginTimeout(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        // Logout
        authManager.logout(uuid);

        // Cancel timeout
        if (loginTimeouts.containsKey(uuid)) {
            loginTimeouts.get(uuid).cancel();
            loginTimeouts.remove(uuid);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Block di chuyển nếu chưa login
        if (!authManager.isLoggedIn(player.getUniqueId())) {
            // Chỉ cancel nếu thực sự di chuyển
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // Block chat nếu chưa login (trừ /register và /login)
        if (!authManager.isLoggedIn(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§cVui lòng đăng nhập trước khi chat!");
        }
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        // Cho phép commands auth
        if (command.startsWith("/register") ||
                command.startsWith("/login")) {
            return;
        }

        // Block các commands khác nếu chưa login
        if (!authManager.isLoggedIn(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§cVui lòng đăng nhập trước!");
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // Block damage nếu chưa login
        if (!authManager.isLoggedIn(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Timeout: Kick player nếu không login trong 60s
     */
    private void startLoginTimeout(Player player) {
        UUID uuid = player.getUniqueId();

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!authManager.isLoggedIn(uuid) && player.isOnline()) {
                player.kickPlayer("§cBạn đã bị kick do không đăng nhập trong 60 giây!");
            }
            loginTimeouts.remove(uuid);
        }, 1200L); // 60 giây = 1200 ticks

        loginTimeouts.put(uuid, task);
    }
}
