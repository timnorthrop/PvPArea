package com.timnorthrop.pvparea;

import org.bukkit.World;

import java.util.UUID;

public record AreaChunkKey(UUID worldId, long chunkKey) {
    static AreaChunkKey fromBlock(World world, int blockX, int blockZ) {
        return fromChunk(world, blockX >> 4, blockZ >> 4);
    }

    static AreaChunkKey fromChunk(World world, int chunkX, int chunkZ) {
        return new AreaChunkKey(world.getUID(), chunkKey(chunkX, chunkZ));
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xffffffffL) | (((long) chunkZ & 0xffffffffL) << 32);
    }
}
