package com.dathuynh.simpleskyblock.managers;

import com.dathuynh.simpleskyblock.Main;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class NetherZoneManager {

    private Main plugin;
    private World netherWorld;

    // Nether zone coordinates (paste location)
    private static final int NETHER_PASTE_X = 0;
    private static final int NETHER_PASTE_Y = 10;
    private static final int NETHER_PASTE_Z = 0;

    // Calculated bounds (will be set after reading schematic)
    private int maxX;
    private int maxY;
    private int maxZ;
    private int centerX;
    private int centerY;
    private int centerZ;

    // Warp location (center)
    private Location netherWarpLocation;

    // Schematic dimensions (cached)
    private boolean dimensionsLoaded = false;

    public NetherZoneManager(Main plugin) {
        this.plugin = plugin;
        this.netherWorld = Bukkit.getWorld("world_nether");

        if (netherWorld == null) {
            plugin.getLogger().severe("world_nether không tồn tại!");
            return;
        }

        // Try to load schematic dimensions
        loadSchematicDimensions();

        // Set warp to center + safe height
        netherWarpLocation = new Location(netherWorld,
                centerX + 0.5,
                centerY,
                centerZ + 0.5);

        plugin.getLogger().info("NetherZoneManager initialized!");
        if (dimensionsLoaded) {
            plugin.getLogger().info("  Nether zone bounds: (" + NETHER_PASTE_X + "," + NETHER_PASTE_Y + "," + NETHER_PASTE_Z +
                    ") to (" + maxX + "," + maxY + "," + maxZ + ")");
            plugin.getLogger().info("  Center warp: (" + centerX + "," + centerY + "," + centerZ + ")");
        }
    }

    /**
     * Load schematic dimensions from file to calculate bounds
     */
    private void loadSchematicDimensions() {
        String schematicName = "mrc93_netherspire.schem";

        // Try multiple locations
        File schematicFile = null;

        // 1. Plugin folder
        File pluginFile = new File(plugin.getDataFolder(), schematicName);
        if (pluginFile.exists()) {
            schematicFile = pluginFile;
        }

        // 2. WorldEdit schematics folder
        if (schematicFile == null) {
            File weFolder = new File("plugins/WorldEdit/schematics");
            File weFile = new File(weFolder, schematicName);
            if (weFile.exists()) {
                schematicFile = weFile;
            }
        }

        // 3. Config WorldEdit folder (Paper 1.20+)
        if (schematicFile == null) {
            File configFolder = new File("config/worldedit/schematics");
            File configFile = new File(configFolder, schematicName);
            if (configFile.exists()) {
                schematicFile = configFile;
            }
        }

        if (schematicFile == null) {
            plugin.getLogger().warning("⚠ Could not find schematic to calculate dimensions!");
            plugin.getLogger().warning("  Using default bounds (60x40x100)");
            // Set default values
            maxX = NETHER_PASTE_X + 60;
            maxY = NETHER_PASTE_Y + 40;
            maxZ = NETHER_PASTE_Z + 100;
            centerX = NETHER_PASTE_X + 27;
            centerY = NETHER_PASTE_Y + 1;
            centerZ = NETHER_PASTE_Z + 45;
            return;
        }

        try {
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
            if (format == null) {
                plugin.getLogger().warning("⚠ Invalid schematic format!");
                setDefaultDimensions();
                return;
            }

            Clipboard clipboard;
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                clipboard = reader.read();
            }

            // Get dimensions from clipboard
            BlockVector3 dimensions = clipboard.getDimensions();
            int sizeX = dimensions.getX();
            int sizeY = dimensions.getY();
            int sizeZ = dimensions.getZ();

            // Calculate max bounds
            maxX = NETHER_PASTE_X + sizeX;
            maxY = NETHER_PASTE_Y + sizeY;
            maxZ = NETHER_PASTE_Z + sizeZ;

            // Calculate center
            centerX = NETHER_PASTE_X + (sizeX / 2);
            centerY = NETHER_PASTE_Y + 1; // 1 block above ground
            centerZ = NETHER_PASTE_Z + (sizeZ / 2);

            dimensionsLoaded = true;

            plugin.getLogger().info("✓ Loaded schematic dimensions: " + sizeX + "x" + sizeY + "x" + sizeZ);

        } catch (IOException e) {
            plugin.getLogger().warning("⚠ Error reading schematic: " + e.getMessage());
            setDefaultDimensions();
        }
    }

    /**
     * Set default dimensions if schematic can't be loaded
     */
    private void setDefaultDimensions() {
        maxX = NETHER_PASTE_X + 60;
        maxY = NETHER_PASTE_Y + 40;
        maxZ = NETHER_PASTE_Z + 100;
        centerX = NETHER_PASTE_X + 27;
        centerY = NETHER_PASTE_Y + 1;
        centerZ = NETHER_PASTE_Z + 45;
    }

    /**
     * Check if nether zone exists
     */
    public boolean isNetherZoneCreated() {
        if (netherWorld == null) return false;

        // Check center block
        Location checkLoc = new Location(netherWorld, centerX, centerY - 1, centerZ);
        return checkLoc.getBlock().getType() != Material.AIR;
    }

    /**
     * Force load chunks to keep mobs alive
     */
    public void forceLoadChunks() {
        if (netherWorld == null) return;

        // Calculate chunk coordinates
        int chunkMinX = NETHER_PASTE_X >> 4;
        int chunkMaxX = maxX >> 4;
        int chunkMinZ = NETHER_PASTE_Z >> 4;
        int chunkMaxZ = maxZ >> 4;

        int chunksLoaded = 0;
        for (int x = chunkMinX - 1; x <= chunkMaxX + 1; x++) {
            for (int z = chunkMinZ - 1; z <= chunkMaxZ + 1; z++) {
                netherWorld.getChunkAt(x, z).setForceLoaded(true);
                chunksLoaded++;
            }
        }

        plugin.getLogger().info("✓ Force-loaded " + chunksLoaded + " chunks in nether zone");
    }

    /**
     * Get warp location
     */
    public Location getNetherWarpLocation() {
        return netherWarpLocation;
    }

    /**
     * Check if location is in nether zone
     */
    public boolean isInNetherZone(Location loc) {
        if (netherWorld == null) return false;
        if (loc.getWorld() == null || !loc.getWorld().equals(netherWorld)) return false;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= NETHER_PASTE_X && x <= maxX &&
                y >= NETHER_PASTE_Y && y <= maxY &&
                z >= NETHER_PASTE_Z && z <= maxZ;
    }

    /**
     * Check if entire nether world
     */
    public boolean isNetherWorld(World world) {
        return netherWorld != null && world != null && world.equals(netherWorld);
    }

    public World getNetherWorld() {
        return netherWorld;
    }

    /**
     * Get schematic dimensions info (for debugging)
     */
    public String getDimensionsInfo() {
        if (!dimensionsLoaded) {
            return "Dimensions not loaded";
        }
        return String.format("Size: %dx%dx%d | Paste at: (%d,%d,%d) | Max: (%d,%d,%d) | Center: (%d,%d,%d)",
                maxX - NETHER_PASTE_X, maxY - NETHER_PASTE_Y, maxZ - NETHER_PASTE_Z,
                NETHER_PASTE_X, NETHER_PASTE_Y, NETHER_PASTE_Z,
                maxX, maxY, maxZ,
                centerX, centerY, centerZ
        );
    }
}
