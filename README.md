# PvPArea

A Minecraft Paper server plugin that enables keepInventory and keepLevel within designated PvP areas.

### Requirements:

Server must have Paper installed.

### Features:

- Commands:
    - ```/pvparea list```: available to all players - lists all active PvP areas
    - ```/pvparea clear-all```: operator required - removes all active areas
    - ```/pvparea add <x-min> <x-max> <z-min> <z-max>```: operator required - adds an area with the given bounds (
      inclusive)
- When players die within a PvP area, they respawn at their normal respawn point, retaining all inventory items and
  experience.
- No inventory items or experience are dropped upon death within a PvP area.
- Players are notified that they died within an area and that keepInventory and keepLevel were enabled.

### Limitations:

- Currently only works in the overworld by grabbing the first world from ```Bukkit.getWorlds()``` whose
  ```environment``` is ```World.Environment.NORMAL```.
- No current functionality for removing a single area from the list of active areas because of the way areas are saved
  as a single array of integers in the ```PersistentDataContainer```.
    - A future update might change the way these integers are saved so that a single area can more easily be removed
      from the list.
- Designed for a small server and will not scale. A Set is scanned each time a player dies, checking to see if the death
  occured in any of the active areas. With more players or a high number of areas, this could be time consuming.
    - In future, to achieve O(1) lookup time, will replace the Set with a Map, mapping each chunk that overlaps an area
      to that area. On death, only if the player is in one of these mapped chunks will coordinates be checked to see if
      they are within the corresponding area.

### What's new in 1.0.1:

- Bumped Paper dependency
