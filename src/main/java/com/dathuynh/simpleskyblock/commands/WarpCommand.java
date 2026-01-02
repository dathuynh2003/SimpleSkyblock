package com.dathuynh.simpleskyblock.commands;

import com.dathuynh.simpleskyblock.managers.ArenaManager;
import com.dathuynh.simpleskyblock.managers.MiningZoneManager;
import com.dathuynh.simpleskyblock.managers.SpawnManager;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WarpCommand implements CommandExecutor {

    private MiningZoneManager miningZoneManager;
    private SpawnManager spawnManager;
    private ArenaManager arenaManager;

    public WarpCommand(MiningZoneManager miningZoneManager, SpawnManager spawnManager, ArenaManager arenaManager) {
        this.miningZoneManager = miningZoneManager;
        this.spawnManager = spawnManager;
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§e═══════════════════════════════");
            player.sendMessage("§6⚡ Danh sách warp:");
            player.sendMessage("§7  /warp lobby §f- Lobby spawn");
            player.sendMessage("§7  /warp khumine §f- Khu mine PvP");
            player.sendMessage("§7  /warp arena1 §f- Boss arena");
            player.sendMessage("§e═══════════════════════════════");
            return true;
        }

        String warpName = args[0].toLowerCase();

        switch (warpName) {
            case "lobby":
            case "spawn":
                player.teleport(spawnManager.getSpawnLocation());
                player.sendMessage("§a✓ Đã teleport về Lobby!");
                break;

            case "khumine":
            case "mine":
                Location mineLoc = miningZoneManager.getMiningWarpLocation();
                if (mineLoc == null) {
                    player.sendMessage("§c✗ Khu mine chưa được khởi tạo!");
                    return true;
                }
                player.teleport(mineLoc);
                player.sendMessage("§a✓ Đã teleport đến Khu Mine!");
                player.sendMessage("§c⚠ Cảnh báo: PvP được bật, hãy cẩn thận!");
                break;

            case "arena1":
                if (!arenaManager.isArena1Created()) {
                    player.sendMessage("§c✗ Arena1 chưa được tạo!");
                    player.sendMessage("§7Admin dùng: §e/init arena1");
                    return true;
                }

                Location arenaLoc = arenaManager.getArena1Center();
                if (arenaLoc != null) {
                    player.teleport(arenaLoc);
                    player.sendMessage("§a✓ Đã teleport tới §cArena1§a!");
                    player.sendMessage("§7⚠ Không có PvP và không rơi items khi chết!");
                } else {
                    player.sendMessage("§c✗ Arena1 không khả dụng!");
                }
                break;

            default:
                player.sendMessage("§c✗ Warp không tồn tại: §e" + warpName);
                player.sendMessage("§7Dùng §e/warp §7để xem danh sách");
                break;
        }

        return true;
    }
}
