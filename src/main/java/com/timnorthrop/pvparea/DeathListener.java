package com.timnorthrop.pvparea;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;
import java.util.Set;
import java.util.logging.Logger;

public class DeathListener implements Listener {
    private Set<PvPArea> areaSet;

    public DeathListener(Set<PvPArea> areaSet) {
        this.areaSet = areaSet;
    }

    public void setAreaSet(Set<PvPArea> areaSet) {
        this.areaSet = areaSet;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        for (PvPArea area : areaSet) {
            if (area.hasPlayerWithin(player) && player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                event.setKeepInventory(true);
                event.getDrops().clear();

                event.setKeepLevel(true);
                event.setDroppedExp(0);


                player.sendRichMessage("You died in a PvP area, so keepInventory and keepLevel were enabled.");
            }
        }
    }
}
