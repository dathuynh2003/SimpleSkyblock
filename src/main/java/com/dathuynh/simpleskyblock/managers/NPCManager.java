package com.dathuynh.simpleskyblock.managers;

import com.dathuynh.simpleskyblock.Main;
import com.dathuynh.simpleskyblock.models.NPCData;
import com.dathuynh.simpleskyblock.models.TradeData;
import com.dathuynh.simpleskyblock.utils.ConfigLoader;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NPCManager implements Listener {

    private Main plugin;
    private ConfigLoader configLoader;
    private HashMap<UUID, String> npcTypes = new HashMap<>();
    private File npcDataFile;
    private FileConfiguration npcData;

    public NPCManager(Main plugin, ConfigLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        setupDataFile();
        loadNPCData();
        markExistingNPCs();
    }

    private void setupDataFile() {
        npcDataFile = new File(plugin.getDataFolder(), "npcs.yml");
        if (!npcDataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                npcDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        npcData = YamlConfiguration.loadConfiguration(npcDataFile);
    }

    private void loadNPCData() {
        if (npcData.contains("npcs")) {
            for (String uuidString : npcData.getConfigurationSection("npcs").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidString);
                String type = npcData.getString("npcs." + uuidString + ".type");
                npcTypes.put(uuid, type);
            }
            plugin.getLogger().info("Đã load " + npcTypes.size() + " NPCs từ file");
        }
    }

    private void markExistingNPCs() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Villager) {
                    Villager villager = (Villager) entity;
                    if (npcTypes.containsKey(villager.getUniqueId())) {
                        villager.setAI(false);
                        villager.setInvulnerable(true);
                        plugin.getLogger().info("Đã restore NPC: " + villager.getCustomName());
                    }
                }
            }
        }
    }

    private void saveNPCData() {
        npcData.set("npcs", null);
        for (UUID uuid : npcTypes.keySet()) {
            String type = npcTypes.get(uuid);
            npcData.set("npcs." + uuid.toString() + ".type", type);
        }
        try {
            npcData.save(npcDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Không thể save NPC data: " + e.getMessage());
        }
    }

    public void spawnNPC(Location location, String npcType) {
        NPCData npcData = configLoader.getNPCData(npcType);
        if (npcData == null) {
            plugin.getLogger().warning("Unknown NPC type: " + npcType);
            return;
        }

        Villager npc = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        npc.setCustomName(npcData.getDisplayName());
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setProfession(npcData.getProfession());
        npc.setVillagerLevel(npcData.getLevel());

        npcTypes.put(npc.getUniqueId(), npcType);
        saveNPCData();
        plugin.getLogger().info("NPC '" + npcType + "' đã được spawn tại " + location.toString());
    }

    public void removeNPC(UUID uuid) {
        npcTypes.remove(uuid);
        saveNPCData();
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager)) {
            return;
        }

        Villager npc = (Villager) event.getRightClicked();
        String npcType = npcTypes.get(npc.getUniqueId());

        if (npcType != null) {
            event.setCancelled(true);
            openTradeMenu(event.getPlayer(), npcType);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Villager) {
            Villager npc = (Villager) event.getEntity();
            if (npcTypes.containsKey(npc.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    private void openTradeMenu(org.bukkit.entity.Player player, String npcType) {
        NPCData npcData = configLoader.getNPCData(npcType);
        if (npcData == null) {
            player.sendMessage("§cNPC này chưa có menu trade!");
            return;
        }

        Inventory menu = Bukkit.createInventory(null, 54, npcData.getDisplayName());

        // Decor - Fill với glass pane
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int i = 0; i < 54; i++) {
            menu.setItem(i, glass);
        }

        // Đặt các trades vào menu
        for (TradeData trade : npcData.getTrades()) {
            int slot = trade.getGuiSlot();

            // Hiển thị required items
            List<ItemStack> requirements = trade.getRequiredItems();
            if (requirements.size() > 0) {
                menu.setItem(slot - 4, createDisplayItem(requirements.get(0), "§7Cần:"));
            }
            if (requirements.size() > 1) {
                menu.setItem(slot - 3, createDisplayItem(requirements.get(1), "§7+"));
            }

            // Arrow
            menu.setItem(slot - 2, createArrow());

            // Reward item
            menu.setItem(slot, trade.getRewardItem());
        }

        // Info book
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§e§lHướng dẫn");
        List<String> lore = new ArrayList<>();
        lore.add("§7Click vào item muốn đổi");
        lore.add("§7Cần có đủ vật phẩm yêu cầu");
        lore.add("");

        for (TradeData trade : npcData.getTrades()) {
            StringBuilder tradeLine = new StringBuilder("§a" + trade.getId() + ": §7");
            for (int i = 0; i < trade.getRequiredItems().size(); i++) {
                ItemStack req = trade.getRequiredItems().get(i);
                tradeLine.append(req.getAmount()).append("x ").append(getItemName(req));
                if (i < trade.getRequiredItems().size() - 1) {
                    tradeLine.append(" + ");
                }
            }
            tradeLine.append(" → ").append(getItemName(trade.getRewardItem()));
            lore.add(tradeLine.toString());
        }

        infoMeta.setLore(lore);
        info.setItemMeta(infoMeta);
        menu.setItem(49, info);

        player.openInventory(menu);
    }

    private ItemStack createDisplayItem(ItemStack original, String prefix) {
        ItemStack display = original.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            String name = meta.hasDisplayName() ? meta.getDisplayName() :
                    "§f" + original.getType().name().replace("_", " ");
            meta.setDisplayName(prefix + " " + original.getAmount() + "x " + name);
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack createArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        meta.setDisplayName("§e→");
        arrow.setItemMeta(meta);
        return arrow;
    }

    private String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return item.getType().name().replace("_", " ");
    }

    public String getNPCType(UUID uuid) {
        return npcTypes.get(uuid);
    }
}
