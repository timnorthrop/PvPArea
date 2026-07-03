package com.timnorthrop.pvparea;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PvPArea {
    private final int xMin, xMax, zMin, zMax;
    private final Set<AreaChunkKey> chunkKeys;
    private final World world;

    public PvPArea(int xMin, int xMax, int zMin, int zMax, World world) {
        if (xMin >= xMax || zMin >= zMax) {
            throw new IllegalArgumentException("Coordinate minimums must be less than maximums");
        }
        this.xMin = xMin;
        this.xMax = xMax;
        this.zMin = zMin;
        this.zMax = zMax;
        this.world = world;
        Set<AreaChunkKey> keys = new HashSet<>();

        int minChunkX = xMin >> 4;
        int maxChunkX = xMax >> 4;
        int minChunkZ = zMin >> 4;
        int maxChunkZ = zMax >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                keys.add(AreaChunkKey.fromChunk(world, chunkX, chunkZ));
            }
        }

        chunkKeys = Collections.unmodifiableSet(keys);
    }

    public boolean hasPlayerWithin(Player player) {
        Location playerLoc = player.getLocation();
        int pX = playerLoc.getBlockX();
        int pZ = playerLoc.getBlockZ();

        return player.getWorld().getKey().equals(world.getKey()) &&
                pX >= xMin && pX <= xMax && pZ >= zMin && pZ <= zMax;
    }

    public boolean overlapsArea(PvPArea other) {
        if (this.equals(other)) {
            return true;
        }
        return world.getUID().equals(other.getWorld().getUID()) &&
                xMin <= other.getXMax() &&
                xMax >= other.getXMin() &&
                zMin <= other.getZMax() &&
                zMax >= other.getZMin();
    }

    public boolean overlapsNoAreas(Map<AreaChunkKey, Set<PvPArea>> others) {
        for (AreaChunkKey ck : chunkKeys) {
            Set<PvPArea> areasInChunk = others.get(ck);
            if (areasInChunk != null) {
                for (PvPArea a : areasInChunk) {
                    if (overlapsArea(a)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public int getXMin() {
        return xMin;
    }

    public int getXMax() {
        return xMax;
    }

    public int getZMin() {
        return zMin;
    }

    public int getZMax() {
        return zMax;
    }

    public World getWorld() {
        return world;
    }

    public Set<AreaChunkKey> getChunkKeys() {
        return chunkKeys;
    }

    @Override
    public String toString() {
        return "(" + world.getKey() +
                ", x-min=" + xMin +
                ", x-max=" + xMax +
                ", z-min=" + zMin +
                ", z-max=" + zMax +
                ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof PvPArea area)) {
            return false;
        }

        return world.getKey().equals(area.getWorld().getKey()) &&
                xMin == area.getXMin() &&
                xMax == area.getXMax() &&
                zMin == area.getZMin() &&
                zMax == area.getZMax();
    }

    @Override
    public int hashCode() {
        return Objects.hash(world.getKey(), xMin, xMax, zMin, zMax);
    }
}
