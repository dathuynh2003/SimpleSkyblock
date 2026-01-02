package com.dathuynh.simpleskyblock.listeners;

import com.dathuynh.simpleskyblock.managers.ArenaManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class ArenaProtection implements Listener {

    private ArenaManager arenaManager;

    public ArenaProtection(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    /**
     * Chặn PvP trong arena
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        // Chỉ check nếu cả attacker và victim đều là player
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        // Nếu victim ở trong arena → cancel PvP
        if (arenaManager.isInArena(victim.getLocation())) {
            event.setCancelled(true);
            attacker.sendMessage("§cKhông thể PvP trong Arena!");
        }
    }

    /**
     * Giữ items khi chết trong arena
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Nếu chết trong arena
        if (arenaManager.isInArena(player.getLocation())) {
            // Giữ items + XP
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.getDrops().clear();
            event.setDroppedExp(0);

//            player.sendMessage("§a✓ Bạn đã giữ items vì chết trong Arena!");
        }
    }
}
