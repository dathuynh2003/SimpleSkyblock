package com.dathuynh.simpleskyblock.commands;

import com.dathuynh.simpleskyblock.managers.NPCManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.util.RayTraceResult;

public class NPCCommand implements CommandExecutor {

    private NPCManager npcManager;

    public NPCCommand(NPCManager npcManager) {
        this.npcManager = npcManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage("§cChỉ admin (OP) mới có thể quản lý NPC!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§6=== NPC Commands ===");
            player.sendMessage("§e/npc spawn <type> §7- Spawn NPC");
            player.sendMessage("§e/npc remove §7- Xóa NPC đang nhìn");
            player.sendMessage("§e/npc list §7- Danh sách NPC types");
            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            if (args.length < 2) {
                player.sendMessage("§cCần chỉ định loại NPC! Dùng: /npc spawn <type>");
                player.sendMessage("§7Dùng /npc list để xem danh sách");
                return true;
            }

            String npcType = args[1].toLowerCase();
            npcManager.spawnNPC(player.getLocation(), npcType);
            player.sendMessage("§aĐã spawn NPC type '" + npcType + "'!");
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            Entity target = getTargetEntity(player, 5.0);

            if (target instanceof Villager) {
                Villager villager = (Villager) target;
                npcManager.removeNPC(villager.getUniqueId());
                villager.remove();
                player.sendMessage("§aĐã xóa NPC!");
            } else {
                player.sendMessage("§cHãy nhìn vào NPC muốn xóa (trong tầm 5 blocks)!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            player.sendMessage("§6=== Available NPC Types ===");
            player.sendMessage("§eTất cả NPCs được config trong §fnpcs_config.yml");
            player.sendMessage("");
            player.sendMessage("§7Ví dụ:");
            player.sendMessage("§e- thorentanthu §7- Thợ Rèn Tân Thủ");
            player.sendMessage("§e- thorenvukhi §7- Thợ Rèn Vũ Khí");
            player.sendMessage("§e- thieunang §7- Thiện Nàng");
            return true;
        }

        return true;
    }

    private Entity getTargetEntity(Player player, double range) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                entity -> entity != player
        );

        if (result != null && result.getHitEntity() != null) {
            return result.getHitEntity();
        }

        return null;
    }
}
