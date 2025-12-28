package com.dathuynh.simpleskyblock.managers;

import com.dathuynh.simpleskyblock.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class SpawnManager {

    private static Location spawnLobby;
    private static boolean isCreated = false;
    private Main plugin;

    public SpawnManager(Main plugin) {
        this.plugin = plugin;
        setupSpawnLobby();
    }

    private void setupSpawnLobby() {
        World world = Bukkit.getWorld("world");
        spawnLobby = new Location(world, 0.5, 100, 0.5);

        // Kiểm tra xem đã tạo chưa
        if (!isCreated && world.getBlockAt(0, 99, 0).getType() != Material.STONE) {
            createSpawnPlatform(world);
            plugin.getLogger().info("Spawn lobby đã được tạo tại (0, 100, 0)!");
        }

        // Set world spawn location
        world.setSpawnLocation(spawnLobby);
        isCreated = true;
    }

    private void createSpawnPlatform(World world) {
        // Tạo platform spawn 7x7
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                world.getBlockAt(x, 99, z).setType(Material.STONE);
            }
        }

        // Viền platform bằng stone bricks
        for (int x = -3; x <= 3; x++) {
            world.getBlockAt(x, 99, -3).setType(Material.STONE_BRICKS);
            world.getBlockAt(x, 99, 3).setType(Material.STONE_BRICKS);
        }
        for (int z = -3; z <= 3; z++) {
            world.getBlockAt(-3, 99, z).setType(Material.STONE_BRICKS);
            world.getBlockAt(3, 99, z).setType(Material.STONE_BRICKS);
        }

        // Tạo beacon base (iron blocks 3x3)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                world.getBlockAt(x, 98, z).setType(Material.IRON_BLOCK);
            }
        }

        // Đặt beacon ở giữa
        world.getBlockAt(0, 100, 0).setType(Material.BEACON);
    }

    public Location getSpawnLocation() {
        return spawnLobby;
    }

    public static boolean isInSpawnArea(Location loc) {
        if (spawnLobby == null) return false;

        return loc.getWorld().equals(spawnLobby.getWorld()) &&
                Math.abs(loc.getX() - spawnLobby.getX()) <= 5 &&
                Math.abs(loc.getZ() - spawnLobby.getZ()) <= 5 &&
                Math.abs(loc.getY() - spawnLobby.getY()) <= 5;
    }
}
