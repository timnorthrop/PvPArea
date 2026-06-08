package com.timnorthrop.pvparea;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class PvPArea {
    private final int xMin, xMax, zMin, zMax;

    public PvPArea(int xMin, int xMax, int zMin, int zMax) {
        if (xMin >= xMax || zMin >= zMax) {
            throw new RuntimeException("Coordinate minimums must be less than maximums");
        }
        this.xMin = xMin;
        this.xMax = xMax;
        this.zMin = zMin;
        this.zMax = zMax;
    }

    public boolean hasPlayerWithin(Player player) {
        Location playerLoc = player.getLocation();
        int pX = playerLoc.getBlockX();
        int pZ = playerLoc.getBlockZ();

        return pX >= xMin && pX <= xMax && pZ >= zMin && pZ <= zMax;
    }

    public boolean overlapsArea(PvPArea other) {
        if (this.equals(other)) {
            return true;
        }
        return !(zMin >= other.getZMax() && zMax > other.getZMax()) &&
                !(zMin < other.getZMin() && zMax <= other.getZMin()) &&
                !(xMin >= other.getXMax() && xMax > other.getXMax()) &&
                !(xMin < other.getXMin() && xMax <= other.getXMin());
    }

    public boolean overlapsAnyArea(Set<PvPArea> others) {
        for (PvPArea other : others) {
            if (this.overlapsArea(other)) {
                return true;
            }
        }
        return false;
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

    @Override
    public String toString() {
        return "(x-min=" + xMin + ", x-max=" + xMax + ", z-min=" + zMin + ", z-max=" + zMax + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof PvPArea area)) {
            return false;
        }

        return this.xMin == area.getXMin() && this.xMax == area.getXMax()
                && this.zMin == area.getZMin() && this.zMax == area.getZMax();
    }

    @Override
    public int hashCode() {
        return Objects.hash(xMin, xMax, zMin, zMax);
    }
}
