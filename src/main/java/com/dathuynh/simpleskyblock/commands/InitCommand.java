package com.dathuynh.simpleskyblock.commands;

import com.dathuynh.simpleskyblock.managers.ArenaManager;
import com.dathuynh.simpleskyblock.managers.MiningZoneManager;
import com.dathuynh.simpleskyblock.managers.SpawnManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InitCommand implements CommandExecutor {

    private SpawnManager spawnManager;
    private MiningZoneManager miningZoneManager;
    private ArenaManager arenaManager;

    public InitCommand(SpawnManager spawnManager, MiningZoneManager miningZoneManager, ArenaManager arenaManager) {
        this.spawnManager = spawnManager;
        this.miningZoneManager = miningZoneManager;
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Check quyền admin
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.isOp()) {
                player.sendMessage("§cChỉ admin mới có thể sử dụng lệnh này!");
                return true;
            }
        }

        // Check arguments
        if (args.length == 0) {
            sender.sendMessage("§e═══════════════════════════════════════════");
            sender.sendMessage("§6⚙ §eLệnh khởi tạo server:");
            sender.sendMessage("§7  /init spawnlobby §f- Paste lobby schematic");
            sender.sendMessage("§7  /init khumine §f- Reset khu mine");
            sender.sendMessage("§7  /init arena1 §f- Create boss arena");
            sender.sendMessage("§e═══════════════════════════════════════════");
            return true;
        }

        // Xử lý subcommand
        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "spawnlobby":
                sender.sendMessage("§e⏳ Đang khởi tạo spawn lobby...");
                sender.sendMessage("§7⚠ Server sẽ lag trong 10-15 giây!");
                spawnManager.pasteSchematicManually();
                break;

            case "khumine":
            case "mine":
                sender.sendMessage("§e⏳ Đang reset Khu Mine...");
                sender.sendMessage("§7⚠ Quá trình này có thể mất 1-2 phút!");
                miningZoneManager.initializeMiningZone();
                break;

            case "arena1":
                if (arenaManager.isArena1Created()) {
                    sender.sendMessage("§e⚠ Arena1 đã tồn tại! Bỏ qua...");
                    return true;
                }

                sender.sendMessage("§e⏳ Đang tạo Arena1...");
                sender.sendMessage("§7⚠ Server sẽ lag trong 5-10 giây!");

                arenaManager.createArena1(() -> {
                    sender.sendMessage("§a✓ Arena1 đã được tạo!");
                    sender.sendMessage("§7Sử dụng §e/warp arena1 §7để tới đó!");
                });
                break;

            default:
                sender.sendMessage("§c❌ Subcommand không hợp lệ!");
                sender.sendMessage("§7Sử dụng: §e/init [spawnlobby|khumine|arena1]");
                break;
        }

        return true;
    }
}
