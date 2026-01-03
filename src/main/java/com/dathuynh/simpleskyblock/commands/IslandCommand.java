package com.dathuynh.simpleskyblock.commands;

import com.dathuynh.simpleskyblock.Main;
import com.dathuynh.simpleskyblock.managers.IslandManager;
import com.dathuynh.simpleskyblock.models.Island;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class IslandCommand implements CommandExecutor {

    private Main plugin;
    private IslandManager islandManager;

    public IslandCommand(Main plugin, IslandManager islandManager) {
        this.plugin = plugin;
        this.islandManager = islandManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này!");
            return true;
        }

        Player player = (Player) sender;

        // /is hoặc /is home - về đảo
        if (args.length == 0 || args[0].equalsIgnoreCase("home")) {
            Location home = islandManager.getIslandHome(player.getUniqueId());
            if (home == null) {
                showHelp(player);
                return true;
            }

            player.teleport(home);
            player.sendMessage("§aĐã dịch chuyển về đảo!");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreate(player);
                break;
            case "delete":
                handleDelete(player);
                break;
            case "info":
                handleInfo(player);
                break;
            case "invite":
                handleInvite(player, args);
                break;
            case "accept":
                handleAccept(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "sethome": // ✅ Thêm lệnh mới
                handleSetHome(player);
                break;
            default:
                showHelp(player);
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§6=== SimpleSkyblock Commands ===");
        player.sendMessage("§e/is §7hoặc §e/is home §7- Về đảo của bạn");
        player.sendMessage("§e/is create §7- Tạo đảo mới");
        player.sendMessage("§e/is delete §7- Xóa đảo (24h cooldown)");
        player.sendMessage("§e/is info §7- Thông tin đảo");
        player.sendMessage("§e/is sethome §7- Đặt điểm spawn mới (chủ đảo)");
        player.sendMessage("§e/is invite <tên> §7- Mời người chơi vào đảo");
        player.sendMessage("§e/is accept <tên> §7- Chấp nhận lời mời");
        player.sendMessage("§e/is leave §7- Rời khỏi đảo (chỉ member)");
    }

    private void handleLeave(Player player) {
        boolean success = islandManager.leaveIsland(player.getUniqueId());

        if (!success) {
            player.sendMessage("§cBạn không thể rời đảo! (Chủ đảo không thể rời hoặc bạn chưa có đảo)");
            return;
        }

        player.sendMessage("§aĐã rời khỏi đảo!");
        player.performCommand("spawn");
    }

    private void handleCreate(Player player) {
        boolean success = islandManager.createIsland(player.getUniqueId(), player.getName());

        if (!success) {
            long cooldown = islandManager.getRemainingCooldown(player.getUniqueId());
            if (cooldown > 0) {
                long hoursLeft = cooldown / (60 * 60 * 1000);
                player.sendMessage("§cBạn phải đợi " + hoursLeft + " giờ nữa để tạo đảo mới!");
            } else {
                player.sendMessage("§cBạn đã có đảo rồi! Dùng /is delete để xóa đảo cũ.");
            }
            return;
        }

        Location home = islandManager.getIslandHome(player.getUniqueId());
        player.teleport(home);
        player.sendMessage("§aĐảo của bạn đã được tạo!");
        player.sendMessage("§eKiểm tra rương để nhận items khởi đầu!");
        player.sendMessage("§7Giới hạn xây dựng: 100x100 blocks");
    }

    private void handleDelete(Player player) {
        boolean success = islandManager.deleteIsland(player.getUniqueId());

        if (!success) {
            player.sendMessage("§cBạn không có đảo hoặc không phải chủ đảo!");
            return;
        }

        player.sendMessage("§aĐã xóa đảo của bạn!");
        player.sendMessage("§eĐợi 24 giờ để tạo đảo mới.");
        player.performCommand("spawn");
    }

    private void handleInfo(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId());

        if (island == null) {
            player.sendMessage("§cBạn chưa có đảo!");
            return;
        }

        Location loc = island.getLocation();
        String ownerName = Bukkit.getOfflinePlayer(island.getOwner()).getName();

        player.sendMessage("§6=== Thông tin đảo ===");
        player.sendMessage("§eChủ đảo: §f" + ownerName);
        player.sendMessage("§eTọa độ: §f" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        player.sendMessage("§eThành viên: §f" + island.getMembers().size());

        long daysSince = (System.currentTimeMillis() - island.getCreatedTime()) / (24 * 60 * 60 * 1000);
        player.sendMessage("§eTuổi đảo: §f" + daysSince + " ngày");
        player.sendMessage("§eGiới hạn: §f100x100 blocks");
    }

    private void handleSetHome(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId());

        if (island == null) {
            player.sendMessage("§cBạn chưa có đảo!");
            return;
        }

        if (!island.isOwner(player.getUniqueId())) {
            player.sendMessage("§cChỉ chủ đảo mới có thể đặt spawn point!");
            return;
        }

        Location playerLoc = player.getLocation();

        // Check có trong island không
        if (!islandManager.isLocationInIsland(playerLoc, island.getLocation())) {
            player.sendMessage("§cBạn phải đứng trong đảo của mình để đặt spawn point!");
            return;
        }

        boolean success = islandManager.setIslandHome(player.getUniqueId(), playerLoc);

        if (success) {
            player.sendMessage("§a✓ Đã đặt spawn point mới tại vị trí hiện tại!");
            player.sendMessage("§7Tọa độ: §e" + playerLoc.getBlockX() + ", " +
                    playerLoc.getBlockY() + ", " + playerLoc.getBlockZ());
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        } else {
            player.sendMessage("§cKhông thể đặt spawn point!");
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cSử dụng: /is invite <tên người chơi>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cNgười chơi không online!");
            return;
        }

        boolean success = islandManager.invitePlayer(player.getUniqueId(), target.getUniqueId());

        if (!success) {
            player.sendMessage("§cKhông thể mời! Bạn phải là chủ đảo và người được mời chưa có đảo.");
            return;
        }

        player.sendMessage("§aĐã gửi lời mời đến " + target.getName());
        target.sendMessage("§e" + player.getName() + " đã mời bạn vào đảo!");
        target.sendMessage("§aDùng §f/is accept " + player.getName() + " §ađể chấp nhận");
    }

    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cSử dụng: /is accept <tên người mời>");
            return;
        }

        Player inviter = Bukkit.getPlayer(args[1]);
        if (inviter == null) {
            player.sendMessage("§cNgười chơi không online!");
            return;
        }

        boolean success = islandManager.acceptInvite(player.getUniqueId(), inviter.getUniqueId());

        if (!success) {
            player.sendMessage("§cKhông có lời mời từ người chơi này hoặc bạn đã có đảo!");
            return;
        }

        player.sendMessage("§aĐã tham gia đảo của " + inviter.getName() + "!");
        inviter.sendMessage("§e" + player.getName() + " đã tham gia đảo của bạn!");

        Location home = islandManager.getIslandHome(player.getUniqueId());
        player.teleport(home);
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }
}
