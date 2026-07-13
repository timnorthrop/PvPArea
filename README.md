# PvPArea

A Minecraft Paper server plugin that enables keepInventory and keepLevel within designated PvP areas.

### What's new in version 1.1.0:
- Added support for multiple worlds! PvP areas can now be added to any world on your server and are stored in the respective `PersistentDataContainer`.
- Added backwards compatibility: this plugin is now officially supported for all stable Paper builds built for versions `1.21.11` through `26.2` of Minecraft.
- Increased performance significantly, especially for servers with many areas or players.
    - Added `AreaChunkKey`, containing a `World` and the chunk coordinates packed into a `long`.  Coordinates will be checked to see if the death occurred in a PvP area only if the `AreaChunkKey` of the chunk the player has died in is present in the plugin's `Map` of areas.

### Requirements:

Server must have Paper installed.

### Features:

*   Commands:
    *   `/pvparea list`: available to all players - lists all active PvP areas
    *   `/pvparea clear-all`: operator required - removes all active areas
    *   `/pvparea add <x-min> <x-max> <z-min> <z-max> <world>`: operator required - adds an area with the given bounds (inclusive) in the given world
*   When players die within a PvP area, they respawn at their normal respawn point, retaining all inventory items and experience.
*   No inventory items or experience are dropped upon death within a PvP area.
*   Players are notified that they died within an area and that keepInventory and keepLevel were enabled.

### Limitations:

*   No current functionality for removing a single area from the list of active areas because of the way areas are saved as a single array of integers in the `PersistentDataContainer`.
    *   A future update might change the way these integers are saved so that a single area can more easily be removed from the list.
