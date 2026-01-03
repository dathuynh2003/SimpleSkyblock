package com.dathuynh.simpleskyblock.commands;

import com.dathuynh.simpleskyblock.Main;
import com.dathuynh.simpleskyblock.managers.NPCManager;
import com.dathuynh.simpleskyblock.models.NPCData;
import com.dathuynh.simpleskyblock.utils.ConfigLoader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.util.RayTraceResult;

public class NPCCommand implements CommandExecutor {

    private Main plugin;
    private NPCManager npcManager;
    private ConfigLoader configLoader;

    public NPCCommand(Main plugin, NPCManager npcManager, ConfigLoader configLoader) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.configLoader = configLoader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage("§cOnly admins (OP) can manage NPCs!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§6=== NPC Commands ===");
            player.sendMessage("§e/npc spawn <type> §7- Spawn NPC");
            player.sendMessage("§e/npc remove §7- Remove targeted NPC");
            player.sendMessage("§e/npc list §7- List all NPC types");
            player.sendMessage("§e/npc reload §7- Reload configs");
            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            if (args.length < 2) {
                player.sendMessage("§cSpecify NPC type! Use: /npc spawn <type>");
                player.sendMessage("§7Use /npc list to see available types");
                return true;
            }

            String npcType = args[1].toLowerCase();
            npcManager.spawnNPC(player.getLocation(), npcType);
            player.sendMessage("§aSpawned NPC type '" + npcType + "'!");
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            Entity target = getTargetEntity(player, 5.0);

            if (target instanceof Villager) {
                Villager villager = (Villager) target;
                npcManager.removeNPC(villager.getUniqueId());
                villager.remove();
                player.sendMessage("§aRemoved NPC!");
            } else {
                player.sendMessage("§cLook at an NPC within 5 blocks!");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            player.sendMessage("§6=== Available NPC Types ===");
            player.sendMessage("§7All NPCs configured in §fnpcs_config.yml");
            player.sendMessage("");

            for (String npcId : configLoader.getAllNPCData().keySet()) {
                NPCData npcData = configLoader.getNPCData(npcId);
                player.sendMessage("§e- " + npcId + " §7- " + npcData.getDisplayName());
            }

            player.sendMessage("");
            player.sendMessage("§7Use: §e/npc spawn <type>");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            player.sendMessage("§eReloading NPC configs...");

            plugin.reloadConfigLoader();

            player.sendMessage("§aSuccessfully reloaded:");
            player.sendMessage("§7- items.yml");
            player.sendMessage("§7- npcs_config.yml");
            player.sendMessage("§7Total NPCs: §e" + configLoader.getAllNPCData().size());

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
