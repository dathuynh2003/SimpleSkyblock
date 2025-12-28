package com.dathuynh.simpleskyblock.commands;

import com.dathuynh.simpleskyblock.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.UUID;

public class IslandCommand implements CommandExecutor {

    private Main plugin;
    private HashMap<UUID, Location> islands = new HashMap<>();
    private HashMap<UUID, Long> islandCreationTime = new HashMap<>();

    // Cấu hình
    private static final int ISLAND_SPACING = 250; // Khoảng cách giữa các đảo (đủ cho 100x100 + buffer)
    private static final int ISLAND_RADIUS = 50; // Bán kính island (100x100 = radius 50)
    private static final long COOLDOWN_DAYS = 7; // 7 ngày
    private static final long COOLDOWN_MS = COOLDOWN_DAYS * 24 * 60 * 60 * 1000L;

    public IslandCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Chỉ người chơi mới dùng được lệnh này!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§6=== SimpleSkyblock Commands ===");
            player.sendMessage("§e/spawn §7- Về spawn lobby");
            player.sendMessage("§e/is create §7- Tạo đảo mới");
            player.sendMessage("§e/is home §7- Về đảo của bạn");
            player.sendMessage("§e/is delete §7- Xóa đảo (7 ngày cooldown)");
            player.sendMessage("§e/is info §7- Thông tin đảo");
            return true;
        }

        if (args[0].equalsIgnoreCase("create")) {
            createIsland(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("home")) {
            teleportHome(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            deleteIsland(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            showIslandInfo(player);
            return true;
        }

        player.sendMessage("§cLệnh không hợp lệ! Dùng /is để xem danh sách.");
        return true;
    }

    private void createIsland(Player player) {
        UUID uuid = player.getUniqueId();

        // Kiểm tra đã có đảo chưa
        if (islands.containsKey(uuid)) {
            player.sendMessage("§cBạn đã có đảo rồi! Dùng /is delete để xóa đảo cũ.");
            return;
        }

        // Kiểm tra cooldown 7 ngày
        if (islandCreationTime.containsKey(uuid)) {
            long timeSinceLastCreation = System.currentTimeMillis() - islandCreationTime.get(uuid);
            if (timeSinceLastCreation < COOLDOWN_MS) {
                long remainingTime = COOLDOWN_MS - timeSinceLastCreation;
                long daysLeft = remainingTime / (24 * 60 * 60 * 1000);
                long hoursLeft = (remainingTime % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);

                player.sendMessage("§cBạn phải đợi " + daysLeft + " ngày " + hoursLeft + " giờ nữa để tạo đảo mới!");
                return;
            }
        }

        World world = Bukkit.getWorld("world");
        int islandNumber = islands.size() + 1;
        int x = islandNumber * ISLAND_SPACING;
        Location islandLoc = new Location(world, x + 0.5, 100, 0.5);

        // Tạo platform 5x5
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                world.getBlockAt(x + i, 99, j).setType(Material.DIRT);
            }
        }

        world.getBlockAt(x, 99, 0).setType(Material.GRASS_BLOCK);

        // Spawn cây trưởng thành
        Location treeLoc = new Location(world, x, 100, 0);
        world.generateTree(treeLoc, org.bukkit.TreeType.TREE);

        // Tạo rương với items
        Location chestLoc = new Location(world, x + 2, 100, 0);
        world.getBlockAt(chestLoc).setType(Material.CHEST);

        org.bukkit.block.Chest chest = (org.bukkit.block.Chest) world.getBlockAt(chestLoc).getState();
        org.bukkit.inventory.Inventory chestInv = chest.getInventory();

        chestInv.addItem(new ItemStack(Material.ICE, 2));
        chestInv.addItem(new ItemStack(Material.LAVA_BUCKET, 1));
        chestInv.addItem(new ItemStack(Material.MELON_SEEDS, 1));
        chestInv.addItem(new ItemStack(Material.PUMPKIN_SEEDS, 1));
        chestInv.addItem(new ItemStack(Material.SUGAR_CANE, 2));
        chestInv.addItem(new ItemStack(Material.BONE_MEAL, 8));
        chestInv.addItem(new ItemStack(Material.BREAD, 5));

        islands.put(uuid, islandLoc);
        islandCreationTime.put(uuid, System.currentTimeMillis());

        player.teleport(islandLoc);
        player.sendMessage("§aĐảo của bạn đã được tạo!");
        player.sendMessage("§eKiểm tra rương để nhận items khởi đầu!");
        player.sendMessage("§7Giới hạn xây dựng: 100x100 blocks");
    }

    private void teleportHome(Player player) {
        UUID uuid = player.getUniqueId();

        if (!islands.containsKey(uuid)) {
            player.sendMessage("§cBạn chưa có đảo! Dùng /is create để tạo.");
            return;
        }

        player.teleport(islands.get(uuid));
        player.sendMessage("§aĐã dịch chuyển về đảo!");
    }

    private void deleteIsland(Player player) {
        UUID uuid = player.getUniqueId();

        if (!islands.containsKey(uuid)) {
            player.sendMessage("§cBạn không có đảo để xóa!");
            return;
        }

        Location islandCenter = islands.get(uuid);
        World world = islandCenter.getWorld();
        int centerX = islandCenter.getBlockX();
        int centerZ = islandCenter.getBlockZ();

        player.sendMessage("§eĐang xóa đảo...");

        // Xóa toàn bộ blocks trong vùng 100x100, từ y=0 đến y=255
        int blocksCleared = 0;
        for (int x = centerX - ISLAND_RADIUS; x <= centerX + ISLAND_RADIUS; x++) {
            for (int z = centerZ - ISLAND_RADIUS; z <= centerZ + ISLAND_RADIUS; z++) {
                for (int y = 0; y <= 255; y++) {
                    if (world.getBlockAt(x, y, z).getType() != Material.AIR) {
                        world.getBlockAt(x, y, z).setType(Material.AIR);
                        blocksCleared++;
                    }
                }
            }
        }

        islands.remove(uuid);
        // Không xóa islandCreationTime để giữ cooldown

        player.sendMessage("§aĐã xóa đảo của bạn! (" + blocksCleared + " blocks)");
        player.sendMessage("§eĐợi 7 ngày để tạo đảo mới.");

        // Teleport về spawn
        player.performCommand("spawn");
    }

    private void showIslandInfo(Player player) {
        UUID uuid = player.getUniqueId();

        if (!islands.containsKey(uuid)) {
            player.sendMessage("§cBạn chưa có đảo!");
            return;
        }

        Location islandLoc = islands.get(uuid);
        player.sendMessage("§6=== Thông tin đảo ===");
        player.sendMessage("§eTọa độ: §f" + islandLoc.getBlockX() + ", " + islandLoc.getBlockY() + ", " + islandLoc.getBlockZ());
        player.sendMessage("§eGiới hạn xây dựng: §f100x100 blocks");

        if (islandCreationTime.containsKey(uuid)) {
            long daysSinceCreation = (System.currentTimeMillis() - islandCreationTime.get(uuid)) / (24 * 60 * 60 * 1000);
            player.sendMessage("§eTuổi đảo: §f" + daysSinceCreation + " ngày");
        }
    }

    // Method để check xem location có nằm trong island của player không
    public boolean isInOwnIsland(Player player, Location loc) {
        UUID uuid = player.getUniqueId();
        if (!islands.containsKey(uuid)) {
            return false;
        }

        Location islandCenter = islands.get(uuid);
        int dx = Math.abs(loc.getBlockX() - islandCenter.getBlockX());
        int dz = Math.abs(loc.getBlockZ() - islandCenter.getBlockZ());

        return dx <= ISLAND_RADIUS && dz <= ISLAND_RADIUS;
    }

    // Method để check xem location có nằm trong island của ai đó không
    public static boolean isInAnyIsland(Location loc, HashMap<UUID, Location> islands) {
        for (Location islandCenter : islands.values()) {
            if (!loc.getWorld().equals(islandCenter.getWorld())) continue;

            int dx = Math.abs(loc.getBlockX() - islandCenter.getBlockX());
            int dz = Math.abs(loc.getBlockZ() - islandCenter.getBlockZ());

            if (dx <= ISLAND_RADIUS && dz <= ISLAND_RADIUS) {
                return true;
            }
        }
        return false;
    }

    public HashMap<UUID, Location> getIslands() {
        return islands;
    }
}
