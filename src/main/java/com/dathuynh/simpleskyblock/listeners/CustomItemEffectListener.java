package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.Main;
import com.dathuynh.simpleskyblock.models.CustomItem;
import com.dathuynh.simpleskyblock.utils.ConfigLoader;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class CustomItemEffectListener implements Listener {

    private Main plugin;
    private ConfigLoader configLoader;

    public CustomItemEffectListener(Main plugin, ConfigLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        startEffectCheckTask();
    }

    private void startEffectCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    updatePlayerEffects(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerEffects(event.getPlayer());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> updatePlayerEffects(event.getPlayer()), 1L);
    }

    private void updatePlayerEffects(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        applyItemEffects(player, helmet);
        applyItemEffects(player, chestplate);
        applyItemEffects(player, leggings);
        applyItemEffects(player, boots);

        // Hand slots
        applyItemEffects(player, player.getInventory().getItemInMainHand());
        applyItemEffects(player, player.getInventory().getItemInOffHand());
    }

    private void applyItemEffects(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().hasDisplayName()) return;

        String displayName = item.getItemMeta().getDisplayName();

        for (CustomItem customItem : configLoader.getAllCustomItems().values()) {
            if (customItem.getDisplayName().equals(displayName)) {
                for (Map.Entry<PotionEffectType, Integer> effect : customItem.getPotionEffects().entrySet()) {
                    player.addPotionEffect(
                            new PotionEffect(effect.getKey(), 40, effect.getValue(), false, false),
                            true
                    );
                }
            }
        }
    }
}
