package com.dathuynh.simpleskyblock.managers;

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
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;

public class SchematicManager {

    private JavaPlugin plugin;
    private Clipboard islandTemplate;

    public SchematicManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadIslandTemplate();
    }

    private void loadIslandTemplate() {
        // Thử load từ plugin folder trước
        File schematicFile = new File(plugin.getDataFolder(), "island_starter.schem");

        // Nếu không có, thử WorldEdit folder
        if (!schematicFile.exists()) {
            schematicFile = new File("plugins/WorldEdit/schematics/island_starter.schem");
        }

        if (!schematicFile.exists()) {
            plugin.getLogger().warning("⚠ Không tìm thấy island_starter.schem!");
            plugin.getLogger().warning("→ Đặt file vào: plugins/WorldEdit/schematics/ hoặc " + plugin.getDataFolder().getPath());
            return;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);
        if (format == null) {
            plugin.getLogger().severe("❌ File schematic không hợp lệ!");
            return;
        }

        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            islandTemplate = reader.read();
            plugin.getLogger().info("✔ Đã load island template thành công!");
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Lỗi khi load schematic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean pasteIsland(Location location) {
        if (islandTemplate == null) {
            return false;
        }

        try {
            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(location.getWorld());

            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                Operation operation = new ClipboardHolder(islandTemplate)
                        .createPaste(editSession)
                        .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                        .ignoreAirBlocks(false)
                        .build();

                Operations.complete(operation);
                return true;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("❌ Lỗi khi paste island: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean hasTemplate() {
        return islandTemplate != null;
    }
}
