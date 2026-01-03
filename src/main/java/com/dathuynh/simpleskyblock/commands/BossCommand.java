package com.dathuynh.simpleskyblock.commands;

import com.dathuynh.simpleskyblock.managers.BossManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BossCommand implements CommandExecutor {

    private BossManager bossManager;

    public BossCommand(BossManager bossManager) {
        this.bossManager = bossManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e═══════════════════════════════");
            sender.sendMessage("§6⚔ §eBoss Commands:");
            sender.sendMessage("§7  /boss info §f- Xem thông tin boss");
            sender.sendMessage("§7  /boss spawn §f- Spawn boss (OP)");
            sender.sendMessage("§7  /boss reload §f- Reload config (OP)");
            sender.sendMessage("§e═══════════════════════════════");
            return true;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "info":
                sender.sendMessage("§e═══════════════════════════════");
                sender.sendMessage("§6⚔ §eThông tin Boss:");

                if (bossManager.isBossAlive()) {
                    sender.sendMessage("§a✓ Trạng thái: §eSống");
                    sender.sendMessage("§7Vị trí: §eArena1 §7(/warp arena1)");

                    // Show health
                    double health = bossManager.getBoss().getHealth();
                    double maxHealth = bossManager.getBoss().getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                    sender.sendMessage(String.format("§7Máu: §c%.0f§7/§c%.0f", health, maxHealth));
                } else {
                    sender.sendMessage("§c✗ Trạng thái: §7Đã chết");
                    sender.sendMessage("§7Hồi sinh sau: " + bossManager.getRespawnTimeFormatted());
                }

                sender.sendMessage("§e═══════════════════════════════");
                break;

            case "spawn":
                if (!(sender instanceof Player) || !((Player) sender).isOp()) {
                    sender.sendMessage("§cChỉ OP mới dùng được lệnh này!");
                    return true;
                }

                if (bossManager.isBossAlive()) {
                    sender.sendMessage("§c✗ Boss đã tồn tại!");
                    return true;
                }

                bossManager.spawnBoss();
                sender.sendMessage("§a✓ Đã spawn boss!");
                break;
            case "reload":
                if (!(sender instanceof Player) || !((Player) sender).isOp()) {
                    sender.sendMessage("§cChỉ OP mới dùng được lệnh này!");
                    return true;
                }
                bossManager.reloadConfig();
                break;

            default:
                sender.sendMessage("§c✗ Lệnh không hợp lệ!");
                break;
        }

        return true;
    }
}
