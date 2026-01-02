package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.managers.SpawnManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpawnProtection implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Admin (OP) được bypass protection
        if (player.isOp()) {
            return;
        }

        if (SpawnManager.isInSpawnArea(event.getBlock().getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§cKhông thể phá block ở spawn lobby!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Admin (OP) được bypass protection
        if (player.isOp()) {
            return;
        }

        if (SpawnManager.isInSpawnArea(event.getBlock().getLocation())) {
            event.setCancelled(true);
            player.sendMessage("§cKhông thể đặt block ở spawn lobby!");
        }
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player victim = (Player) event.getEntity();
            Player attacker = (Player) event.getDamager();

            if (SpawnManager.isInSpawnArea(victim.getLocation()) ||
                    SpawnManager.isInSpawnArea(attacker.getLocation())) {
                event.setCancelled(true);
                attacker.sendMessage("§cKhông thể PvP ở spawn lobby!");
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        //Chỉ check khi player thực sự di chuyển
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        if (SpawnManager.isInSpawnArea(player.getLocation())) {
            if (!player.hasPotionEffect(PotionEffectType.SPEED)) {
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED, 200, 1, false, false
                )); // Tăng duration lên 200 ticks (10s)
            }
        }
    }

}
