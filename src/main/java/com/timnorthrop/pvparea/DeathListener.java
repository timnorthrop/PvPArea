package com.timnorthrop.pvparea;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;

import java.util.Set;

public class DeathListener implements Listener {
    private final PvPAreaPlugin plugin;

    public DeathListener(PvPAreaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getPlayer();
        final World pWorld = player.getWorld();
        final int x = player.getLocation().getBlockX();
        final int z = player.getLocation().getBlockZ();
        final long ck = pWorld.getChunkAt(x, z).getChunkKey();

        if (plugin.getAreaMap().containsKey(ck)) {
            final Set<PvPArea> areasInChunk = plugin.getAreaMap().get(ck);
            for (PvPArea a : areasInChunk) {
                if (a.hasPlayerWithin(player)) {
                    event.setKeepInventory(true);
                    event.getDrops().clear();

                    event.setKeepLevel(true);
                    event.setDroppedExp(0);

                    player.sendMessage("You died in a PvP area, so keepInventory and keepLevel were enabled.");
                    plugin.getLogger().info(player.getName() + " died in a PvP area at " +
                            "(" + x + ", " + player.getLocation().getBlockY() + ", " + z + ").");

                    return;
                }
            }
        }
    }
}
