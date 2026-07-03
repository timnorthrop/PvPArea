package com.timnorthrop.pvparea;

import org.bukkit.Location;
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
        final Location location = player.getLocation();
        final int x = location.getBlockX();
        final int z = location.getBlockZ();
        final AreaChunkKey ck = AreaChunkKey.fromBlock(location.getWorld(), x, z);
        final Set<PvPArea> areasInChunk = plugin.getAreaMap().get(ck);

        if (areasInChunk != null) {
            for (PvPArea a : areasInChunk) {
                if (a.hasPlayerWithin(player)) {
                    event.setKeepInventory(true);
                    event.getDrops().clear();

                    event.setKeepLevel(true);
                    event.setDroppedExp(0);

                    player.sendMessage("You died in a PvP area, so keepInventory and keepLevel were enabled.");
                    plugin.getLogger().info(player.getName() + " died in a PvP area at " +
                            "(" + x + ", " + location.getBlockY() + ", " + z + ").");

                    return;
                }
            }
        }
    }
}
