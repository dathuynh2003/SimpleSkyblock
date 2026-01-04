package com.dathuynh.simpleskyblock.commands;

import com.dathuynh.simpleskyblock.managers.ArenaManager;
import com.dathuynh.simpleskyblock.managers.MiningZoneManager;
import com.dathuynh.simpleskyblock.managers.NetherZoneManager;
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
    private NetherZoneManager netherZoneManager;

    public WarpCommand(MiningZoneManager miningZoneManager, SpawnManager spawnManager, ArenaManager arenaManager, NetherZoneManager netherZoneManager) {
        this.miningZoneManager = miningZoneManager;
        this.spawnManager = spawnManager;
        this.arenaManager = arenaManager;
        this.netherZoneManager = netherZoneManager;
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
            player.sendMessage("§6Danh sách warp:");
            player.sendMessage("§7  /warp lobby - Lobby spawn");
            player.sendMessage("§7  /warp khumine - Khu mine PvP §c(5 level)");
            player.sendMessage("§7  /warp arena1 - Boss arena");
            player.sendMessage("§7  /warp nether - Teleport to Nether §c(5 level)");
            player.sendMessage("§e═══════════════════════════════");
            return true;
        }

        String warpName = args[0].toLowerCase();

        switch (warpName) {
            case "lobby":
            case "spawn":
                player.teleport(spawnManager.getSpawnLocation());
                player.sendMessage("§aĐã teleport về Lobby!");
                break;

            case "khumine":
            case "mine":
                if (player.getLevel() < 5) {
                    player.sendMessage("§cBạn cần ít nhất 5 level để warp đến Khu Mine!");
                    player.sendMessage("§7Level hiện tại: §e" + player.getLevel());
                    return true;
                }

                Location mineLoc = miningZoneManager.getMiningWarpLocation();
                if (mineLoc == null) {
                    player.sendMessage("§cKhu mine chưa được khởi tạo!");
                    return true;
                }

                player.setLevel(player.getLevel() - 5);
                player.teleport(mineLoc);
                player.sendMessage("§aĐã teleport đến Khu Mine! §7(-5 level)");
                player.sendMessage("§cCảnh báo: PvP được bật, hãy cẩn thận!");
                break;

            case "arena1":
                if (!arenaManager.isArena1Created()) {
                    player.sendMessage("§cArena1 chưa được tạo!");
                    player.sendMessage("§7Admin dùng: §e/init arena1");
                    return true;
                }

                Location arenaLoc = arenaManager.getArena1Center();
                if (arenaLoc != null) {
                    player.teleport(arenaLoc);
                    player.sendMessage("§aĐã teleport tới §cArena1§a!");
                    player.sendMessage("§7Không có PvP và không rơi items khi chết!");
                } else {
                    player.sendMessage("§cArena1 không khả dụng!");
                }
                break;

            case "nether":
                if (!netherZoneManager.isNetherZoneCreated()) {
                    player.sendMessage("§cKhu vực Nether chưa được tạo!");
                    player.sendMessage("§7Admin sử dụng: §e/init nether");
                    return true;
                }

                if (player.getLevel() < 5) {
                    player.sendMessage("§cBạn cần ít nhất 5 level để warp đến Nether!");
                    player.sendMessage("§7Level hiện tại: §e" + player.getLevel());
                    return true;
                }

                Location netherLoc = netherZoneManager.getNetherWarpLocation();
                player.setLevel(player.getLevel() - 5);
                player.teleport(netherLoc);
                player.sendMessage("§aĐã teleport đến Nether Zone! §7(-5 level)");
                player.sendMessage("§7Khu vực PvP tắt, chết không rơi đồ!");
                break;

            default:
                player.sendMessage("§cWarp không tồn tại: §e" + warpName);
                player.sendMessage("§7Dùng §e/warp §7để xem danh sách");
                break;
        }

        return true;
    }
}
