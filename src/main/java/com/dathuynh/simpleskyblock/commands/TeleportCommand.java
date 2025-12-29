package com.dathuynh.simpleskyblock.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportCommand implements CommandExecutor {

    private Map<UUID, UUID> pendingRequests; // target -> requester
    private static final long REQUEST_TIMEOUT = 60000; // 60 giây
    private Map<UUID, Long> requestTime;

    public TeleportCommand() {
        this.pendingRequests = new HashMap<>();
        this.requestTime = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§6=== Teleport Commands ===");
            player.sendMessage("§e/tp <tên> §7- Xin teleport đến người chơi");
            player.sendMessage("§e/tp accept <tên> §7- Chấp nhận yêu cầu teleport");
            return true;
        }

        if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                player.sendMessage("§cSử dụng: /tp accept <tên người chơi>");
                return true;
            }
            handleAccept(player, args[1]);
            return true;
        }

        // /tp <player> - request teleport
        handleRequest(player, args[0]);
        return true;
    }

    private void handleRequest(Player requester, String targetName) {
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            requester.sendMessage("§cNgười chơi không online!");
            return;
        }

        if (target.equals(requester)) {
            requester.sendMessage("§cBạn không thể teleport đến chính mình!");
            return;
        }

        pendingRequests.put(target.getUniqueId(), requester.getUniqueId());
        requestTime.put(target.getUniqueId(), System.currentTimeMillis());

        requester.sendMessage("§aĐã gửi yêu cầu teleport đến " + target.getName());
        target.sendMessage("§e" + requester.getName() + " muốn teleport đến bạn!");
        target.sendMessage("§aDùng §f/tp accept " + requester.getName() + " §ađể chấp nhận");
    }

    private void handleAccept(Player target, String requesterName) {
        UUID requesterUuid = pendingRequests.get(target.getUniqueId());

        if (requesterUuid == null) {
            target.sendMessage("§cKhông có yêu cầu teleport nào!");
            return;
        }

        Player requester = Bukkit.getPlayer(requesterUuid);

        if (requester == null) {
            target.sendMessage("§cNgười chơi không còn online!");
            pendingRequests.remove(target.getUniqueId());
            requestTime.remove(target.getUniqueId());
            return;
        }

        // ✅ FIX: Validate tên người request
        if (!requester.getName().equalsIgnoreCase(requesterName)) {
            target.sendMessage("§cKhông có yêu cầu teleport từ " + requesterName + "!");
            target.sendMessage("§eYêu cầu hiện tại từ: §f" + requester.getName());
            return;
        }

        // Check timeout
        Long timeRequested = requestTime.get(target.getUniqueId());
        if (timeRequested != null && System.currentTimeMillis() - timeRequested > REQUEST_TIMEOUT) {
            pendingRequests.remove(target.getUniqueId());
            requestTime.remove(target.getUniqueId());
            target.sendMessage("§cYêu cầu teleport đã hết hạn!");
            return;
        }

        requester.teleport(target.getLocation());
        requester.sendMessage("§aĐã teleport đến " + target.getName());
        target.sendMessage("§aĐã chấp nhận yêu cầu teleport từ " + requester.getName());

        pendingRequests.remove(target.getUniqueId());
        requestTime.remove(target.getUniqueId());
    }
}
