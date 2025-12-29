package com.dathuynh.simpleskyblock.commands;

import com.dathuynh.simpleskyblock.managers.IslandManager;
import com.dathuynh.simpleskyblock.utils.KitConfig;
import com.dathuynh.simpleskyblock.utils.KitConfig.Kit;
import com.dathuynh.simpleskyblock.utils.KitConfig.UsageType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class KitCommand implements CommandExecutor {

    private JavaPlugin plugin;
    private IslandManager islandManager;
    private KitConfig kitConfig;

    // Tracking kit usage
    private Map<UUID, Set<String>> usedOnceKits;  // Player UUID -> Set of kit IDs used once
    private Map<String, Map<UUID, Long>> cooldownKits;  // Kit ID -> (Player UUID -> Last use timestamp)

    public KitCommand(JavaPlugin plugin, IslandManager islandManager, KitConfig kitConfig) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.kitConfig = kitConfig;
        this.usedOnceKits = new HashMap<>();
        this.cooldownKits = new HashMap<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cChỉ người chơi mới dùng được lệnh này!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showKitList(player);
            return true;
        }

        String kitId = args[0].toLowerCase();
        Kit kit = kitConfig.getKit(kitId);

        if (kit == null) {
            player.sendMessage("§cKit không tồn tại! Dùng §e/kit §cđể xem danh sách.");
            return true;
        }

        claimKit(player, kit);
        return true;
    }

    private void showKitList(Player player) {
        player.sendMessage("§6§l=== DANH SÁCH KIT ===");
        player.sendMessage("");

        for (Kit kit : kitConfig.getAllKits().values()) {
            String status = getKitStatus(player, kit);
            player.sendMessage("§e/kit " + kit.getId() + " §7- " + kit.getDisplayName());
            player.sendMessage("  §7" + kit.getDescription());
            player.sendMessage("  " + status);
            player.sendMessage("");
        }
    }

    private String getKitStatus(Player player, Kit kit) {
        UUID uuid = player.getUniqueId();

        if (kit.getUsageType() == UsageType.ONCE) {
            if (hasUsedOnce(uuid, kit.getId())) {
                return "§c✗ Đã sử dụng (1 lần duy nhất)";
            } else {
                return "§a✓ Có thể nhận (1 lần)";
            }
        } else {
            long remaining = getRemainingCooldown(uuid, kit.getId(), kit.getCooldownMs());
            if (remaining > 0) {
                long hours = remaining / (60 * 60 * 1000);
                long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);
                return "§c⏰ Còn " + hours + "h " + minutes + "m";
            } else {
                return "§a✓ Có thể nhận (mỗi " + kit.getCooldownMs() / (60 * 60 * 1000) + "h)";
            }
        }
    }

    private void claimKit(Player player, Kit kit) {
        UUID uuid = player.getUniqueId();

        // Check require island
        if (kit.requiresIsland() && islandManager.getIsland(uuid) == null) {
            player.sendMessage("§cBạn phải có đảo mới được nhận kit này! Dùng §e/is create");
            return;
        }

        // Check usage type
        if (kit.getUsageType() == UsageType.ONCE) {
            if (hasUsedOnce(uuid, kit.getId())) {
                player.sendMessage("§cBạn đã sử dụng kit này rồi! (Chỉ được dùng 1 lần)");
                return;
            }
        } else {
            long remaining = getRemainingCooldown(uuid, kit.getId(), kit.getCooldownMs());
            if (remaining > 0) {
                long hours = remaining / (60 * 60 * 1000);
                long minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000);
                player.sendMessage("§cBạn phải đợi §e" + hours + " giờ " + minutes + " phút §cnữa!");
                return;
            }
        }

        // Check inventory space
        int slotsNeeded = kit.getItems().size();
        int emptySlots = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) {
                emptySlots++;
            }
        }

        if (emptySlots < slotsNeeded) {
            player.sendMessage("§cKhông đủ chỗ trong túi! Cần ít nhất §e" + slotsNeeded + " slot §ctrống.");
            return;
        }

        // Give items
        for (ItemStack item : kit.getItems()) {
            player.getInventory().addItem(item.clone());
        }

        // Track usage
        if (kit.getUsageType() == UsageType.ONCE) {
            markUsedOnce(uuid, kit.getId());
        } else {
            setCooldown(uuid, kit.getId(), System.currentTimeMillis());
        }

        // Messages
        player.sendMessage("§a§l✔ Đã nhận " + kit.getDisplayName() + "§a§l!");
        player.sendMessage("§7" + kit.getDescription());
    }

    // Helper methods
    private boolean hasUsedOnce(UUID uuid, String kitId) {
        return usedOnceKits.containsKey(uuid) && usedOnceKits.get(uuid).contains(kitId);
    }

    private void markUsedOnce(UUID uuid, String kitId) {
        usedOnceKits.computeIfAbsent(uuid, k -> new HashSet<>()).add(kitId);
    }

    private long getRemainingCooldown(UUID uuid, String kitId, long cooldownMs) {
        if (!cooldownKits.containsKey(kitId)) return 0;

        Map<UUID, Long> playerCooldowns = cooldownKits.get(kitId);
        if (!playerCooldowns.containsKey(uuid)) return 0;

        long lastUse = playerCooldowns.get(uuid);
        long elapsed = System.currentTimeMillis() - lastUse;

        return Math.max(0, cooldownMs - elapsed);
    }

    private void setCooldown(UUID uuid, String kitId, long timestamp) {
        cooldownKits.computeIfAbsent(kitId, k -> new HashMap<>()).put(uuid, timestamp);
    }

    // Getters for save/load
    public Map<UUID, Set<String>> getUsedOnceKits() {
        return usedOnceKits;
    }

    public Map<String, Map<UUID, Long>> getCooldownKits() {
        return cooldownKits;
    }

    public void setUsedOnceKits(Map<UUID, Set<String>> data) {
        this.usedOnceKits = data;
    }

    public void setCooldownKits(Map<String, Map<UUID, Long>> data) {
        this.cooldownKits = data;
    }
}
