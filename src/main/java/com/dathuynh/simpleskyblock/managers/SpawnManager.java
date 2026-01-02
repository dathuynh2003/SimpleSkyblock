package com.dathuynh.simpleskyblock.managers;

import com.dathuynh.simpleskyblock.Main;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.SideEffectSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SpawnManager {
    private static final int CENTER_LOBBY_X = 69;
    private static final int CENTER_LOBBY_Z = 143;
    private static final int CENTER_LOBBY_Y = -51;

    private static Location spawnLobby;
    private static World lobbyWorld;
    private Main plugin;

    public SpawnManager(Main plugin) {
        this.plugin = plugin;
        setupSpawnLobby();
    }

    private void setupSpawnLobby() {
        // 1. Kiểm tra world_lobby đã tồn tại chưa
        File worldFolder = new File(Bukkit.getWorldContainer(), "world_lobby");
        boolean worldExists = worldFolder.exists() && new File(worldFolder, "level.dat").exists();

        // 2. Load/Create world_lobby
        lobbyWorld = Bukkit.getWorld("world_lobby");

        if (lobbyWorld == null) {
            if (!worldExists) {
                plugin.getLogger().info("World lobby chưa tồn tại, đang tạo void world...");
            } else {
                plugin.getLogger().info("World lobby đã tồn tại, đang load...");
            }

            WorldCreator creator = new WorldCreator("world_lobby");
            creator.generator("VoidWorldGenerator");
            lobbyWorld = creator.createWorld();
            plugin.getLogger().info("Đã load world_lobby!");
        }

        // 3. Set spawn location
        spawnLobby = new Location(lobbyWorld, CENTER_LOBBY_X + 0.5, 100 + CENTER_LOBBY_Y, CENTER_LOBBY_Z + 0.5);
        lobbyWorld.setSpawnLocation(spawnLobby);

        // 4. Check lobby đã paste chưa
        if (lobbyWorld.getBlockAt(CENTER_LOBBY_X, 100 + CENTER_LOBBY_Y - 1, CENTER_LOBBY_Z).getType().isAir()) {
            plugin.getLogger().warning("═══════════════════════════════════════════");
            plugin.getLogger().warning(" LOBBY CHƯA ĐƯỢC PASTE!");
            plugin.getLogger().warning(" Sử dụng lệnh: /init spawnlobby");
            plugin.getLogger().warning(" (Chỉ cần chạy 1 lần duy nhất)");
            plugin.getLogger().warning("═══════════════════════════════════════════");
        } else {
            plugin.getLogger().info("Lobby schematic đã sẵn sàng!");
        }
    }

    public void pasteSchematicManually() {
        File schematicFile = new File(plugin.getDataFolder(), "world_lobby.schem");

        if (!schematicFile.exists()) {
            plugin.getLogger().warning("Không tìm thấy file: world_lobby.schem");
            plugin.getLogger().warning("  Đặt file vào: " + schematicFile.getAbsolutePath());
            return;
        }

        plugin.getLogger().info("Bắt đầu paste lobby schematic...");
        plugin.getLogger().info("Server sẽ lag trong 10-15 giây, vui lòng đợi...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
                if (format == null) {
                    plugin.getLogger().severe("Không nhận diện được format schematic!");
                    return;
                }

                Clipboard clipboard;
                try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                    clipboard = reader.read();
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    try (EditSession editSession = WorldEdit.getInstance()
                            .newEditSession(BukkitAdapter.adapt(lobbyWorld))) {

                        editSession.setFastMode(true);
                        editSession.setSideEffectApplier(SideEffectSet.none());

                        Operation operation = new ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(BlockVector3.at(0, 100, 0))
                                .ignoreAirBlocks(false)
                                .copyBiomes(false)
                                .copyEntities(false)
                                .build();

                        Operations.complete(operation);

                        plugin.getLogger().info("═══════════════════════════════════════════");
                        plugin.getLogger().info("Đã paste lobby schematic thành công!");
                        plugin.getLogger().info("Vị trí: (0, 100, 0) trong world_lobby");
                        plugin.getLogger().info("═══════════════════════════════════════════");

                    } catch (Exception e) {
                        plugin.getLogger().severe("Lỗi paste: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

            } catch (IOException e) {
                plugin.getLogger().severe("Lỗi load schematic: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public Location getSpawnLocation() {
        return spawnLobby;
    }

    public World getLobbyWorld() {
        return lobbyWorld;
    }

    /**
     * Kiểm tra location có trong spawn area không
     * Spawn area = Toàn bộ world_lobby NGOẠI TRỪ khu mine
     */
    public static boolean isInSpawnArea(Location loc) {
        if (spawnLobby == null || lobbyWorld == null) return false;
        if (loc.getWorld() == null || !loc.getWorld().equals(lobbyWorld)) return false;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        // Check nếu ở trong khu mine → KHÔNG phải spawn area
        // Khu mine: (5000, 100, 5000) với size 51x60x51
        int mineMinX = 5000;
        int mineMaxX = mineMinX + 51 + 1;
        int mineMinY = 100 - 1;   // MINE_Y - 1 (đáy bedrock)
        int mineMaxY = 100 + 60;  // MINE_Y + WALL_HEIGHT
        int mineMinZ = 5000;
        int mineMaxZ = mineMinZ + 51 + 1;

        boolean isInMine = x >= mineMinX && x <= mineMaxX &&
                y >= mineMinY && y <= mineMaxY &&
                z >= mineMinZ && z <= mineMaxZ;

        // Spawn area = world_lobby NHƯNG không phải khu mine
        return !isInMine;
    }

    public static boolean isLobbyWorld(World world) {
        return lobbyWorld != null && world != null && world.equals(lobbyWorld);
    }
}
