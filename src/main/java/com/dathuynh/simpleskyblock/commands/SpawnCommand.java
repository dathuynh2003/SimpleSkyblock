package com.dathuynh.simpleskyblock.commands;

import com.dathuynh.simpleskyblock.managers.SpawnManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private SpawnManager spawnManager;

    public SpawnCommand(SpawnManager spawnManager) {
        this.spawnManager = spawnManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này!");
            return true;
        }

        Player player = (Player) sender;
        player.teleport(spawnManager.getSpawnLocation());
        player.sendMessage("§aĐã dịch chuyển về spawn!");
        return true;
    }
}
