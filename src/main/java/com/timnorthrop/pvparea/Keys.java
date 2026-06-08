package com.timnorthrop.pvparea;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class Keys {

    private Keys() {}

    public static NamespacedKey PVP_AREA;

    public static void init(JavaPlugin plugin) {
        PVP_AREA = new NamespacedKey(plugin, "pvp_area");
    }
}
