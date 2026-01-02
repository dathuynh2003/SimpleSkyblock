package com.dathuynh.simpleskyblock.managers;

import com.dathuynh.simpleskyblock.Main;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MiningZoneManager {
    // Tọa độ center Spawn Lobby
    private static final int CENTER_LOBBY_X = 69;
    private static final int CENTER_LOBBY_Z = 143;
    private static final int CENTER_LOBBY_Y = -51;

    // Tọa độ Mining Zone
    private static final int MINE_X = 5000;
    private static final int MINE_Y = 100;
    private static final int MINE_Z = 5000;

    // Kích thước
    private static final int FLOOR_SIZE = 52; // 52x52 đáy
    private static final int WALL_HEIGHT = 60; // Chiều cao tường
    private static final int INNER_SIZE = 50; // 50x50x50 ruột

    // Tỉ lệ khoáng sản (%)
    private static final double DIAMOND_RATE = 1.0;
    private static final double LAPIS_RATE = 2.0;
    private static final double REDSTONE_RATE = 2.0;
    private static final double ANCIENT_DEBRIS_RATE = 0.25;
    private static final double GOLD_RATE = 3.0;
    private static final double IRON_RATE = 4.0;
    private static final double COPPER_RATE = 4.0;
    private static final double EMERALD_RATE = 2.0;
    private static final double COAL_RATE = 15.0;
    // Còn lại: 66.75% = Cobblestone

    // Thời gian reset
    private static final int TIME_RESET = 12;    //hours

    private Main plugin;
    private World miningWorld;
    private Location miningWarpLocation;
    private BukkitTask autoResetTask;
    private Random random;

    public MiningZoneManager(Main plugin) {
        this.plugin = plugin;
        this.random = new Random();
        setupMiningZone();
        startAutoReset();
    }

    private void setupMiningZone() {
        // Get world_lobby
        miningWorld = Bukkit.getWorld("world_lobby");

        if (miningWorld == null) {
            plugin.getLogger().severe("Không tìm thấy world_lobby cho Mining Zone!");
            return;
        }

        // Set warp location (trung tâm khu mine, trên mặt đất)
        miningWarpLocation = new Location(
                miningWorld,
                MINE_X + 25.5,
                MINE_Y + WALL_HEIGHT + 1,
                MINE_Z + 25.5
        );

        plugin.getLogger().info("Mining Zone đã được setup tại (" + MINE_X + ", " + MINE_Y + ", " + MINE_Z + ")");
    }

    /**
     * Khởi tạo Mining Zone lần đầu hoặc reset
     */
    public void initializeMiningZone() {
        if (miningWorld == null) {
            plugin.getLogger().severe("Mining world chưa được load!");
            return;
        }

        plugin.getLogger().info("Đang khởi tạo Mining Zone...");

        // Chạy async để tránh lag
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            long startTime = System.currentTimeMillis();

            // 1. Tạo đáy (51x51 Bedrock)
            buildFloor();

            // 2. Tạo 4 bức tường (Bedrock)
            buildWalls();

            // 3. Fill ruột bên trong với ores
            fillOres();

            long endTime = System.currentTimeMillis();
            plugin.getLogger().info("Mining Zone đã được khởi tạo! (Thời gian: " + (endTime - startTime) + "ms)");

            // Broadcast cho players
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.broadcastMessage("§6§l[Mining Zone]");
                Bukkit.broadcastMessage("§aKhu mine đã được reset!");
                Bukkit.broadcastMessage("§7Sử dụng §e/warp khumine §7để đến khu mine!");
            });
        });
    }

    private void buildFloor() {
        List<Block> blocksToUpdate = new ArrayList<>();

        // Collect blocks async
        for (int x = 0; x < FLOOR_SIZE; x++) {
            for (int z = 0; z < FLOOR_SIZE; z++) {
                Block block = miningWorld.getBlockAt(MINE_X + x, MINE_Y - 1, MINE_Z + z);
                blocksToUpdate.add(block);
            }
        }

        // Update all blocks in one sync task
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Block block : blocksToUpdate) {
                block.setType(Material.BEDROCK);
            }
        });
    }

    private void buildWalls() {
        List<Block> blocksToUpdate = new ArrayList<>();

        for (int y = 0; y < WALL_HEIGHT; y++) {
            // Tường Bắc
            for (int x = 0; x < FLOOR_SIZE; x++) {
                blocksToUpdate.add(miningWorld.getBlockAt(MINE_X + x, MINE_Y + y, MINE_Z));
            }
            // Tường Nam
            for (int x = 0; x < FLOOR_SIZE; x++) {
                blocksToUpdate.add(miningWorld.getBlockAt(MINE_X + x, MINE_Y + y, MINE_Z + FLOOR_SIZE - 1));
            }
            // Tường Tây
            for (int z = 0; z < FLOOR_SIZE; z++) {
                blocksToUpdate.add(miningWorld.getBlockAt(MINE_X, MINE_Y + y, MINE_Z + z));
            }
            // Tường Đông
            for (int z = 0; z < FLOOR_SIZE; z++) {
                blocksToUpdate.add(miningWorld.getBlockAt(MINE_X + FLOOR_SIZE - 1, MINE_Y + y, MINE_Z + z));
            }
        }

        // Batch update
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Block block : blocksToUpdate) {
                block.setType(Material.BEDROCK);
            }
        });
    }

    private void fillOres() {
        int totalBlocks = INNER_SIZE * INNER_SIZE * INNER_SIZE;
        List<BlockData> blocksToUpdate = new ArrayList<>();

        for (int x = 1; x <= INNER_SIZE; x++) {
            for (int y = 0; y < INNER_SIZE; y++) {
                for (int z = 1; z <= INNER_SIZE; z++) {
                    Material blockType = getRandomOre();
                    Block block = miningWorld.getBlockAt(MINE_X + x, MINE_Y + y, MINE_Z + z);
                    blocksToUpdate.add(new BlockData(block, blockType));
                }
            }
        }

        // Batch update với progress
        final int BATCH_SIZE = 1000;
        final int totalBatches = (blocksToUpdate.size() + BATCH_SIZE - 1) / BATCH_SIZE;

        for (int i = 0; i < totalBatches; i++) {
            final int batchIndex = i;
            final int start = i * BATCH_SIZE;
            final int end = Math.min(start + BATCH_SIZE, blocksToUpdate.size());
            final List<BlockData> batch = blocksToUpdate.subList(start, end);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (BlockData blockData : batch) {
                    blockData.block.setType(blockData.material);
                }

                int progress = ((batchIndex + 1) * 100) / totalBatches;
                plugin.getLogger().info("Đang fill ores: " + progress + "%");

                if (batchIndex == totalBatches - 1) {
                    plugin.getLogger().info("Hoàn thành fill ores!");
                }
            }, i * 2L); // Delay giữa các batch để tránh lag
        }
    }

    // Helper class
    private static class BlockData {
        Block block;
        Material material;

        BlockData(Block block, Material material) {
            this.block = block;
            this.material = material;
        }
    }

    private Material getRandomOre() {
        double roll = random.nextDouble() * 100.0;

        if (roll < DIAMOND_RATE) return Material.DIAMOND_ORE;
        roll -= DIAMOND_RATE;

        if (roll < LAPIS_RATE) return Material.LAPIS_ORE;
        roll -= LAPIS_RATE;

        if (roll < REDSTONE_RATE) return Material.REDSTONE_ORE;
        roll -= REDSTONE_RATE;

        if (roll < ANCIENT_DEBRIS_RATE) return Material.ANCIENT_DEBRIS;
        roll -= ANCIENT_DEBRIS_RATE;

        if (roll < GOLD_RATE) return Material.GOLD_ORE;
        roll -= GOLD_RATE;

        if (roll < IRON_RATE) return Material.IRON_ORE;
        roll -= IRON_RATE;

        if (roll < COPPER_RATE) return Material.COPPER_ORE;
        roll -= COPPER_RATE;

        if (roll < EMERALD_RATE) return Material.EMERALD_ORE;
        roll -= EMERALD_RATE;

        if (roll < COAL_RATE) return Material.COAL_ORE;

        return Material.COBBLESTONE;
    }

    /**
     * Auto Reset
     */
    private void startAutoReset() {
        //
        long interval = TIME_RESET * 60 * 60 * 20;

        autoResetTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            plugin.getLogger().info("Thời gian reset Mining Zone tự động!");

            // Cảnh báo trước 30 giây
            Bukkit.broadcastMessage("§c§l[Cảnh Báo]");
            Bukkit.broadcastMessage("§eKhu mine sẽ được reset sau §c30 giây§e!");
            Bukkit.broadcastMessage("§eVui lòng rời khỏi khu mine ngay!");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Teleport tất cả players ra khỏi khu mine
                kickPlayersFromMine();

                // Reset khu mine
                initializeMiningZone();
            }, 600L); // 30 giây = 600 ticks

        }, interval, interval); // Delay và repeat đều là 2 tiếng

        plugin.getLogger().info("Auto-reset Mining Zone đã được kích hoạt (mỗi 2 tiếng)");
    }

    private void kickPlayersFromMine() {
        Location lobbySpawn = new Location(miningWorld, 0.5 + CENTER_LOBBY_X, 100 + CENTER_LOBBY_Y, 0.5 + CENTER_LOBBY_Z);
        int kickedPlayers = 0;
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            if (isInMiningZone(player.getLocation())) {
                player.teleport(lobbySpawn);
                player.sendMessage("§cBạn đã bị teleport ra khỏi khu mine do reset!");
                kickedPlayers++;
            }
        }
        if (kickedPlayers > 0) {
            plugin.getLogger().info("✓ Đã kick " + kickedPlayers + " players khỏi khu mine");
        }
    }

    /**
     * Kiểm tra location có trong Mining Zone không
     */
    public boolean isInMiningZone(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().equals(miningWorld)) {
            return false;
        }

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= MINE_X && x < MINE_X + FLOOR_SIZE &&
                y >= MINE_Y && y < MINE_Y + WALL_HEIGHT &&
                z >= MINE_Z && z < MINE_Z + FLOOR_SIZE;
    }

    public Location getMiningWarpLocation() {
        return miningWarpLocation;
    }

    public void stopAutoReset() {
        if (autoResetTask != null) {
            autoResetTask.cancel();
            plugin.getLogger().info("Auto-reset Mining Zone đã bị hủy");
        }
    }
}
