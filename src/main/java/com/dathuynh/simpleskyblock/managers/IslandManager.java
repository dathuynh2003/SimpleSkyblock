package com.dathuynh.simpleskyblock.managers;

import com.dathuynh.simpleskyblock.models.Island;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class IslandManager {

    private JavaPlugin plugin;
    private SchematicManager schematicManager;

    private Map<UUID, Island> islands; // UUID player -> Island
    private Map<UUID, UUID> playerToIsland; // UUID player -> UUID island owner
    private Map<UUID, Set<UUID>> pendingInvites; // UUID inviter -> Set of invited players
    private Map<UUID, Long> deletionCooldowns; // UUID player -> deletion timestamp

    private static final int ISLAND_SPACING = 250;
    private static final int ISLAND_RADIUS = 50;
    private static final long COOLDOWN_HOURS = 24;
    private static final long COOLDOWN_MS = COOLDOWN_HOURS * 60 * 60 * 1000L;
    private static final int SPAWN_OFFSET_X = -28;
    private static final int SPAWN_OFFSET_Y = 42;
    private static final int SPAWN_OFFSET_Z = -11;

    private File dataFile;
    private FileConfiguration dataConfig;

    public IslandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schematicManager = new SchematicManager(plugin);

        this.islands = new HashMap<>();
        this.playerToIsland = new HashMap<>();
        this.pendingInvites = new HashMap<>();
        this.deletionCooldowns = new HashMap<>();

        createDataFile();
        loadData();
    }

    private void createDataFile() {
        dataFile = new File(plugin.getDataFolder(), "islands.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        // Lưu islands data vào YAML
        dataConfig.set("islands", null); // Clear old data

        for (Map.Entry<UUID, Island> entry : islands.entrySet()) {
            String path = "islands." + entry.getKey().toString();
            Island island = entry.getValue();

            dataConfig.set(path + ".owner", island.getOwner().toString());
            dataConfig.set(path + ".location", island.getLocation());
            dataConfig.set(path + ".home-location", island.getHomeLocation());

            List<String> membersList = new ArrayList<>();
            for (UUID member : island.getMembers()) {
                membersList.add(member.toString());
            }
            dataConfig.set(path + ".members", membersList);
            dataConfig.set(path + ".created", island.getCreatedTime());
            dataConfig.set(path + ".deleted", island.getDeletedTime());
        }

        // Lưu cooldowns
        for (Map.Entry<UUID, Long> entry : deletionCooldowns.entrySet()) {
            dataConfig.set("cooldowns." + entry.getKey().toString(), entry.getValue());
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadData() {
        if (!dataConfig.contains("islands")) {
            plugin.getLogger().info("Không có dữ liệu island để load.");
            return;
        }

        ConfigurationSection islandsSection = dataConfig.getConfigurationSection("islands");
        if (islandsSection == null) return;

        for (String ownerUuidStr : islandsSection.getKeys(false)) {
            try {
                UUID ownerUuid = UUID.fromString(ownerUuidStr);
                String path = "islands." + ownerUuidStr;

                // Load location
                Location location = dataConfig.getLocation(path + ".location");
                if (location == null) continue;

                // Tạo island object
                Island island = new Island(ownerUuid, location);

                // Load homeLocation
                if (dataConfig.contains(path + ".home-location")) {
                    Location homeLocation = dataConfig.getLocation(path + ".home-location");
                    if (homeLocation != null) {
                        island.setHomeLocation(homeLocation);
                    }
                }

                // Load members
                List<String> membersList = dataConfig.getStringList(path + ".members");
                for (String memberUuidStr : membersList) {
                    UUID memberUuid = UUID.fromString(memberUuidStr);
                    island.getMembers().add(memberUuid);
                    playerToIsland.put(memberUuid, ownerUuid); // KEY: Map mỗi member về island owner
                }

                // Load timestamps
                island.setCreatedTime(dataConfig.getLong(path + ".created"));
                island.setDeletedTime(dataConfig.getLong(path + ".deleted", 0));

                islands.put(ownerUuid, island);

            } catch (Exception e) {
                plugin.getLogger().warning("Lỗi khi load island: " + ownerUuidStr);
                e.printStackTrace();
            }
        }

        // Load cooldowns
        if (dataConfig.contains("cooldowns")) {
            ConfigurationSection cooldownSection = dataConfig.getConfigurationSection("cooldowns");
            if (cooldownSection != null) {
                for (String uuidStr : cooldownSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        long timestamp = dataConfig.getLong("cooldowns." + uuidStr);
                        deletionCooldowns.put(uuid, timestamp);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Lỗi khi load cooldown: " + uuidStr);
                    }
                }
            }
        }

        plugin.getLogger().info("Đã load " + islands.size() + " islands!");
    }

    // Tạo đảo mới
    public boolean createIsland(UUID playerUuid, String playerName) {
        // Check cooldown
        if (deletionCooldowns.containsKey(playerUuid)) {
            long timeSince = System.currentTimeMillis() - deletionCooldowns.get(playerUuid);
            if (timeSince < COOLDOWN_MS) {
                return false; // Còn cooldown
            } else {
                deletionCooldowns.remove(playerUuid);
            }
        }

        // Check đã có đảo chưa
        if (playerToIsland.containsKey(playerUuid)) {
            return false; // Đã có đảo
        }

        World world = Bukkit.getWorld("world");
        int islandNumber = islands.size() + 1;
        int x = islandNumber * ISLAND_SPACING;
        Location islandLoc = new Location(world, x + SPAWN_OFFSET_X, 100 + SPAWN_OFFSET_Y, SPAWN_OFFSET_Z);

        // Generate island structure
        generateIslandStructure(world, x);

        Island island = new Island(playerUuid, islandLoc);
        islands.put(playerUuid, island);
        playerToIsland.put(playerUuid, playerUuid);

        saveData();
        return true;
    }

    private void generateIslandStructure(World world, int x) {
        Location islandLoc = new Location(world, x, 100, 0);

        if (schematicManager.hasTemplate()) {
            boolean success = schematicManager.pasteIsland(islandLoc);

            if (success) {
                plugin.getLogger().info("✔ Đã paste island template tại x=" + x);
                generateBarrierWall(world, x);
                return;
            }
        }

        plugin.getLogger().warning("⚠ Schematic không khả dụng, dùng default platform...");
        generateDefaultPlatform(world, x);
        generateBarrierWall(world, x);
    }

    private void generateDefaultPlatform(World world, int x) {
        // Platform 5x5
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                world.getBlockAt(x + i, 99, j).setType(Material.DIRT);
            }
        }

        world.getBlockAt(x, 99, 0).setType(Material.GRASS_BLOCK);

        // Spawn cây
        Location treeLoc = new Location(world, x, 100, 0);
        world.generateTree(treeLoc, org.bukkit.TreeType.TREE);

        // Tạo rương với items
        Location chestLoc = new Location(world, x + 2, 100, 0);
        world.getBlockAt(chestLoc).setType(Material.CHEST);

        org.bukkit.block.Chest chest = (org.bukkit.block.Chest) world.getBlockAt(chestLoc).getState();
        org.bukkit.inventory.Inventory chestInv = chest.getInventory();

        chestInv.addItem(new org.bukkit.inventory.ItemStack(Material.ICE, 2));
        chestInv.addItem(new org.bukkit.inventory.ItemStack(Material.LAVA_BUCKET, 1));
        chestInv.addItem(new org.bukkit.inventory.ItemStack(Material.MELON_SEEDS, 1));
        chestInv.addItem(new org.bukkit.inventory.ItemStack(Material.PUMPKIN_SEEDS, 1));
        chestInv.addItem(new org.bukkit.inventory.ItemStack(Material.SUGAR_CANE, 2));
        chestInv.addItem(new org.bukkit.inventory.ItemStack(Material.BONE_MEAL, 8));
        chestInv.addItem(new org.bukkit.inventory.ItemStack(Material.BREAD, 5));
    }

    private void generateBarrierWall(World world, int centerX) {
        int centerZ = 0;

        for (int y = 0; y <= 256; y++) {
            // Tường X+ và X- (không bao gồm góc)
            for (int z = -ISLAND_RADIUS + 1; z < ISLAND_RADIUS; z++) {
                world.getBlockAt(centerX + ISLAND_RADIUS, y, centerZ + z).setType(Material.BARRIER);
                world.getBlockAt(centerX - ISLAND_RADIUS, y, centerZ + z).setType(Material.BARRIER);
            }

            // Tường Z+ và Z- (bao gồm góc)
            for (int x = -ISLAND_RADIUS; x <= ISLAND_RADIUS; x++) {
                world.getBlockAt(centerX + x, y, centerZ + ISLAND_RADIUS).setType(Material.BARRIER);
                world.getBlockAt(centerX + x, y, centerZ - ISLAND_RADIUS).setType(Material.BARRIER);
            }
        }
    }

    // Xóa đảo
    public boolean deleteIsland(UUID playerUuid) {
        UUID islandOwnerUuid = playerToIsland.get(playerUuid);
        if (islandOwnerUuid == null) {
            return false; // Không có đảo
        }

        Island island = islands.get(islandOwnerUuid);
        if (!island.isOwner(playerUuid)) {
            return false; // Không phải owner
        }

        // Xóa blocks
        clearIslandBlocks(island.getLocation());

        // Remove tất cả members
        for (UUID member : island.getMembers()) {
            playerToIsland.remove(member);
        }

        islands.remove(islandOwnerUuid);
        deletionCooldowns.put(playerUuid, System.currentTimeMillis());

        saveData();
        return true;
    }

    private void clearIslandBlocks(Location center) {
        World world = center.getWorld();
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();

        // Xóa barrier wall trước
        for (int y = 0; y <= 256; y++) {
            for (int z = -ISLAND_RADIUS + 1; z < ISLAND_RADIUS; z++) {
                world.getBlockAt(centerX + ISLAND_RADIUS, y, centerZ + z).setType(Material.AIR);
                world.getBlockAt(centerX - ISLAND_RADIUS, y, centerZ + z).setType(Material.AIR);
            }

            for (int x = -ISLAND_RADIUS; x <= ISLAND_RADIUS; x++) {
                world.getBlockAt(centerX + x, y, centerZ + ISLAND_RADIUS).setType(Material.AIR);
                world.getBlockAt(centerX + x, y, centerZ - ISLAND_RADIUS).setType(Material.AIR);
            }
        }

        // Sau đó xóa nội dung đảo
        for (int x = centerX - ISLAND_RADIUS + 1; x < centerX + ISLAND_RADIUS; x++) {
            for (int z = centerZ - ISLAND_RADIUS + 1; z < centerZ + ISLAND_RADIUS; z++) {
                for (int y = 0; y <= 128; y++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
    }


    // Invite system
    public boolean invitePlayer(UUID inviter, UUID invited) {
        // Check inviter có đảo và là owner
        UUID islandOwnerUuid = playerToIsland.get(inviter);
        if (islandOwnerUuid == null || !islandOwnerUuid.equals(inviter)) {
            return false;
        }

        // Check invited chưa có đảo
        if (playerToIsland.containsKey(invited)) {
            return false;
        }

        pendingInvites.computeIfAbsent(inviter, k -> new HashSet<>()).add(invited);
        return true;
    }

    public boolean acceptInvite(UUID invited, UUID inviter) {
        // Check có lời mời không
        Set<UUID> invites = pendingInvites.get(inviter);
        if (invites == null || !invites.contains(invited)) {
            return false;
        }

        // Check invited chưa có đảo
        if (playerToIsland.containsKey(invited)) {
            return false;
        }

        Island island = islands.get(inviter);
        if (island == null) {
            return false;
        }

        island.addMember(invited);
        playerToIsland.put(invited, inviter);
        invites.remove(invited);

        saveData();
        return true;
    }

    // Getters
    public Island getIsland(UUID playerUuid) {
        UUID ownerUuid = playerToIsland.get(playerUuid);
        return ownerUuid != null ? islands.get(ownerUuid) : null;
    }

    public Location getIslandHome(UUID playerUuid) {
        Island island = getIsland(playerUuid);
        return island != null ? island.getHomeLocation() : null;
    }

    public boolean setIslandHome(UUID playerUuid, Location newHome) {
        Island island = getIsland(playerUuid);

        if (island == null) {
            return false;
        }

        if (!island.isOwner(playerUuid)) {
            return false; // Chỉ owner mới set được
        }

        // Check location có trong island không
        if (!isLocationInIsland(newHome, island.getLocation())) {
            return false;
        }

        island.setHomeLocation(newHome);
        saveData();
        return true;
    }

    public long getRemainingCooldown(UUID playerUuid) {
        if (!deletionCooldowns.containsKey(playerUuid)) {
            return 0;
        }

        long timeSince = System.currentTimeMillis() - deletionCooldowns.get(playerUuid);
        return Math.max(0, COOLDOWN_MS - timeSince);
    }

    public boolean isInOwnIsland(UUID playerUuid, Location loc) {
        Island island = getIsland(playerUuid);
        if (island == null) return false;

        return isLocationInIsland(loc, island.getLocation());
    }

    public boolean isLocationInIsland(Location loc, Location islandCenter) {
        if (!loc.getWorld().equals(islandCenter.getWorld())) return false;

        int dx = Math.abs(loc.getBlockX() - islandCenter.getBlockX());
        int dz = Math.abs(loc.getBlockZ() - islandCenter.getBlockZ());

        return dx < ISLAND_RADIUS && dz < ISLAND_RADIUS;
    }

    public Collection<Island> getAllIslands() {
        return islands.values();
    }

    public boolean leaveIsland(UUID playerUuid) {
        UUID islandOwnerUuid = playerToIsland.get(playerUuid);

        if (islandOwnerUuid == null) {
            return false; // Không có đảo
        }

        // Không cho phép owner rời đảo
        if (islandOwnerUuid.equals(playerUuid)) {
            return false; // Là owner, phải dùng /is delete
        }

        Island island = islands.get(islandOwnerUuid);
        if (island == null) {
            return false;
        }

        // Remove member
        island.removeMember(playerUuid);
        playerToIsland.remove(playerUuid);

        saveData();
        return true;
    }
}
