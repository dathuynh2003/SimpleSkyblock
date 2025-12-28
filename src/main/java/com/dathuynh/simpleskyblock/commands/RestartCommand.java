package com.dathuynh.simpleskyblock.commands;

import com.dathuynh.simpleskyblock.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RestartCommand implements CommandExecutor {

    private Main plugin;

    public RestartCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // Chỉ OP mới dùng được
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.isOp()) {
                player.sendMessage("§cChỉ admin mới có thể restart server!");
                return true;
            }
        }

        // Countdown 10 giây
        int countdown = 10;

        Bukkit.broadcastMessage("§c§l⚠ SERVER ĐANG RESTART!");
        Bukkit.broadcastMessage("§eServer sẽ restart sau " + countdown + " giây...");

        new BukkitRunnable() {
            int timeLeft = 10;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    Bukkit.broadcastMessage("§c§lRESTARTING...");

                    // Kick tất cả players
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.kickPlayer("§c§lServer Restarting\n§eVui lòng reconnect sau vài giây!");
                    }

                    // Delay 1 giây rồi stop
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Bukkit.shutdown(); // Stop server
                        }
                    }.runTaskLater(plugin, 20L); // 1 giây

                    cancel();
                    return;
                }

                if (timeLeft <= 5) {
                    Bukkit.broadcastMessage("§c§lRestart trong " + timeLeft + " giây!");
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Mỗi 1 giây

        return true;
    }
}
