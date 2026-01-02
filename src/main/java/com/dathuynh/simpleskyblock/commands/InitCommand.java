package com.dathuynh.simpleskyblock.commands;

import com.dathuynh.simpleskyblock.managers.ArenaManager;
import com.dathuynh.simpleskyblock.managers.MiningZoneManager;
import com.dathuynh.simpleskyblock.managers.NetherZoneManager;
import com.dathuynh.simpleskyblock.managers.SpawnManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InitCommand implements CommandExecutor {

    private SpawnManager spawnManager;
    private MiningZoneManager miningZoneManager;
    private ArenaManager arenaManager;
    private NetherZoneManager netherZoneManager;

    public InitCommand(SpawnManager spawnManager, MiningZoneManager miningZoneManager, ArenaManager arenaManager, NetherZoneManager netherZoneManager) {
        this.spawnManager = spawnManager;
        this.miningZoneManager = miningZoneManager;
        this.arenaManager = arenaManager;
        this.netherZoneManager = netherZoneManager;
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
            sender.sendMessage("§7  /init nether §f- Create Nether Zone");
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
            case "nether":
                // Show dimensions info first
                sender.sendMessage("§e📊 Nether Zone Info:");
                sender.sendMessage("§7" + netherZoneManager.getDimensionsInfo());
                sender.sendMessage("");

                if (netherZoneManager.isNetherZoneCreated()) {
                    sender.sendMessage("§a✓ Nether zone đã tồn tại!");
                    sender.sendMessage("§7Chunks đang được force-loaded...");
                    netherZoneManager.forceLoadChunks();
                    sender.sendMessage("§a✓ Hoàn tất! Sử dụng §e/warp nether §ađể đến đó!");
                } else {
                    sender.sendMessage("");
                    sender.sendMessage("§c⚠ Nether zone chưa được paste!");
                    sender.sendMessage("");
                    sender.sendMessage("§6§l═══════════════════════════════════════════");
                    sender.sendMessage("§e§l📋 HƯỚNG DẪN PASTE NETHER ZONE:");
                    sender.sendMessage("§6§l═══════════════════════════════════════════");
                    sender.sendMessage("");
                    sender.sendMessage("§e1. §fĐảm bảo file §esatans-lair.schem §fđã có trong:");
                    sender.sendMessage("   §7plugins/WorldEdit/schematics/  §a← KHUYẾN KHÍCH");
                    sender.sendMessage("   §7plugins/SimpleSkyblock/");
                    sender.sendMessage("   §7config/worldedit/schematics/");
                    sender.sendMessage("");
                    sender.sendMessage("§e2. §fTeleport đến world_nether tại §e(0, 10, 0)§f:");
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        sender.sendMessage("");

                        // Create clickable teleport button
                        net.md_5.bungee.api.chat.TextComponent tpButton = new net.md_5.bungee.api.chat.TextComponent("   §a§l[CLICK ĐỂ TELEPORT]");
                        tpButton.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND,
                                "/execute in minecraft:the_nether run tp @s 0 10 0"
                        ));
                        tpButton.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                                new net.md_5.bungee.api.chat.ComponentBuilder("§eClick để teleport đến world_nether").create()
                        ));
                        player.spigot().sendMessage(tpButton);

                        sender.sendMessage("");
                        sender.sendMessage("   §7Hoặc dùng: §e/execute in minecraft:the_nether run tp @s 0 10 0");
                    } else {
                        sender.sendMessage("   §7(Console không thể teleport, dùng player)");
                    }
                    sender.sendMessage("");
                    sender.sendMessage("§e3. §fLoad schematic:");
                    sender.sendMessage("   §a➜ //schem load satans-lair.schem");
                    sender.sendMessage("");
                    sender.sendMessage("§e4. §fPaste schematic:");
                    sender.sendMessage("   §a➜ //paste -a");
                    sender.sendMessage("");
                    sender.sendMessage("§e5. §fĐợi paste hoàn tất");
                    sender.sendMessage("   §7(Thời gian tùy vào kích thước schematic)");
                    sender.sendMessage("");
                    sender.sendMessage("§e6. §fSau khi paste xong, chạy lại lệnh:");
                    sender.sendMessage("   §a➜ /init nether");
                    sender.sendMessage("   §7(Để force-load chunks và kích hoạt zone)");
                    sender.sendMessage("");
                    sender.sendMessage("§6§l═══════════════════════════════════════════");
                    sender.sendMessage("");
                    sender.sendMessage("§7💡 Lưu ý:");
                    sender.sendMessage("§7- WorldEdit sẽ tối ưu paste tốt hơn plugin");
                    sender.sendMessage("§7- Paste xong có thể dùng §e/warp nether");
                    sender.sendMessage("");
                }
                break;


            default:
                sender.sendMessage("§c❌ Subcommand không hợp lệ!");
                sender.sendMessage("§7Sử dụng: §e/init [spawnlobby|khumine|arena1]");
                break;
        }

        return true;
    }
}
