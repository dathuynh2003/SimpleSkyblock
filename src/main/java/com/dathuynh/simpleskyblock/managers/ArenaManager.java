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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ArenaManager {

    private Main plugin;
    private World arenaWorld;

    // Arena1 coordinates
    private static final int ARENA1_X = 10000;
    private static final int ARENA1_Y = 100;
    private static final int ARENA1_Z = 10000;
    private static final double ARENA1_CENTER_X = ARENA1_X - 31 + 0.5;
    private static final double ARENA1_CENTER_Y = ARENA1_Y - 20;
    private static final double ARENA1_CENTER_Z = ARENA1_Z - 32 + 0.5;
    private static final int ARENA1_SIZE_X = 50;
    private static final int ARENA1_SIZE_Y = 30;
    private static final int ARENA1_SIZE_Z = 50;

    public ArenaManager(Main plugin) {
        this.plugin = plugin;
        this.arenaWorld = Bukkit.getWorld("world_lobby");
    }

    /**
     * Check if arena1 đã được tạo chưa (bằng cách check 1 block)
     */
    public boolean isArena1Created() {
        if (arenaWorld == null) return false;

        // Check center block của arena (thường là solid block, không phải air)
        Location checkLoc = new Location(arenaWorld,
                ARENA1_CENTER_X,
                ARENA1_CENTER_Y - 2,
                ARENA1_CENTER_Z);

        // Nếu block không phải AIR → arena đã paste
        return checkLoc.getBlock().getType() != Material.AIR;
    }

    /**
     * Load schematic file
     */
    private File getSchematicFile(String filename) {
        // Check external file
        File externalFile = new File(plugin.getDataFolder(), filename);
        if (externalFile.exists()) {
            plugin.getLogger().info("✓ Load external schematic: " + filename);
            return externalFile;
        }

        // Extract from resources
        plugin.getLogger().info("→ Extracting schematic from JAR: " + filename);

        try (InputStream in = plugin.getResource(filename)) {
            if (in == null) {
                plugin.getLogger().severe("✗ Schematic not found: " + filename);
                return null;
            }

            File outputFile = new File(plugin.getDataFolder(), filename);
            if (!outputFile.exists()) {
                java.nio.file.Files.copy(in, outputFile.toPath());
                plugin.getLogger().info("✓ Extracted schematic: " + filename);
            }

            return outputFile;
        } catch (IOException e) {
            plugin.getLogger().severe("✗ Error loading schematic: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create arena1
     */
    public void createArena1(Runnable callback) {
        if (isArena1Created()) {
            plugin.getLogger().info("Arena1 đã tồn tại! Bỏ qua...");
            if (callback != null) callback.run();
            return;
        }

        String schematicName = "arena_boss1.schem";
        File schematicFile = getSchematicFile(schematicName);

        if (schematicFile == null) {
            plugin.getLogger().severe("✗ Cannot create arena: schematic not found!");
            return;
        }

        plugin.getLogger().info("⏳ Creating Arena1 from: " + schematicName);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
                if (format == null) {
                    plugin.getLogger().severe("✗ Invalid schematic format!");
                    return;
                }

                Clipboard clipboard;
                try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                    clipboard = reader.read();
                }

                // Paste sync
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try (EditSession editSession = WorldEdit.getInstance()
                            .newEditSession(BukkitAdapter.adapt(arenaWorld))) {

                        editSession.setSideEffectApplier(com.sk89q.worldedit.util.SideEffectSet.none());

                        Operation operation = new ClipboardHolder(clipboard)
                                .createPaste(editSession)
                                .to(BlockVector3.at(ARENA1_X, ARENA1_Y, ARENA1_Z))
                                .ignoreAirBlocks(false)
                                .copyBiomes(false)
                                .copyEntities(false)
                                .build();

                        Operations.complete(operation);

                        plugin.getLogger().info("✓ Arena1 created at ("
                                + ARENA1_X + ", " + ARENA1_Y + ", " + ARENA1_Z + ")");

                        if (callback != null) callback.run();

                    } catch (Exception e) {
                        plugin.getLogger().severe("✗ Error pasting arena: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

            } catch (IOException e) {
                plugin.getLogger().severe("✗ Error loading arena schematic: " + e.getMessage());
            }
        });
    }

    /**
     * Get arena1 center location
     */
    public Location getArena1Center() {
        if (arenaWorld == null) return null;
        return new Location(arenaWorld,
                ARENA1_CENTER_X,
                ARENA1_CENTER_Y,
                ARENA1_CENTER_Z);
    }

    /**
     * Check if location is in arena1
     */
    public boolean isInArena(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().equals(arenaWorld)) {
            return false;
        }

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= ARENA1_X && x <= ARENA1_X + ARENA1_SIZE_X &&
                y >= ARENA1_Y && y <= ARENA1_Y + ARENA1_SIZE_Y &&
                z >= ARENA1_Z && z <= ARENA1_Z + ARENA1_SIZE_Z;
    }
}
