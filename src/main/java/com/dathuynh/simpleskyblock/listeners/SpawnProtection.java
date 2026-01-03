package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.managers.SpawnManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
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

    /**
     * Chặn FALL DAMAGE trong spawn
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        // Chỉ check nếu là player
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        // Chỉ check fall damage
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        Player player = (Player) event.getEntity();

        // Nếu player ở trong spawn → cancel fall damage
        if (SpawnManager.isInSpawnArea(player.getLocation())) {
            event.setCancelled(true);
        }
    }

    /**
     * Giữ items và XP khi chết trong spawn lobby
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Check nếu player chết trong spawn area
        if (SpawnManager.isInSpawnArea(player.getLocation())) {
            // Giữ inventory (items + armor)
            event.setKeepInventory(true);

            // Giữ XP level
            event.setKeepLevel(true);

            // Clear tất cả drops (để không duplicate items)
            event.getDrops().clear();

            // Set dropped XP = 0 (để không rơi XP orbs)
            event.setDroppedExp(0);

            // Optional: Message cho player
            // player.sendMessage("§a✓ Bạn đã giữ items vì chết trong Spawn Lobby!");
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
                ));
            }
        } else {
            if (player.hasPotionEffect(PotionEffectType.SPEED)) {
                PotionEffect currentSpeed = player.getPotionEffect(PotionEffectType.SPEED);
                // Chỉ remove nếu là speed buff từ spawn (level 1, duration < 200)
                if (currentSpeed != null &&
                        currentSpeed.getAmplifier() == 1 &&
                        currentSpeed.getDuration() <= 200) {
                    player.removePotionEffect(PotionEffectType.SPEED);
                }
            }
        }
    }

}
