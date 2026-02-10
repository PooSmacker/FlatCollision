package com.dripps.flatcollision.client.particle;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * divides world into large sectors (64 blocks) with one ring buffer each.
 * sectors created lazily and never freed, just overwrite stale data.
 */
public final class SectorGrid {

    /** sector size in blocks */
    public static final int SECTOR_SIZE = 64;

    /** max particles per sector ring buffer */
    private static final int RING_CAPACITY = 2048;

    /** map from packed sector key to ring buffer */
    private final Long2ObjectOpenHashMap<RingBuffer> sectors = new Long2ObjectOpenHashMap<>();

    /** converts world coordinate to sector coordinate */
    public static int toSectorCoord(double worldCoord) {
        return Math.floorDiv((int) Math.floor(worldCoord), SECTOR_SIZE);
    }

    /** packs two sector coordinates into long key */
    public static long packKey(int sectorX, int sectorZ) {
        return ((long) sectorX << 32) | (sectorZ & 0xFFFFFFFFL);
    }

    /**
     * emits particle into sector at world position. creates sector lazily if needed.
     */
    public int emit(double worldX, double worldY, double worldZ,
                    float vx, float vy, float vz, int lifetime) {
        long key = packKey(toSectorCoord(worldX), toSectorCoord(worldZ));
        RingBuffer ring = sectors.computeIfAbsent(key, k -> new RingBuffer(RING_CAPACITY));
        return ring.emit((float) worldX, (float) worldY, (float) worldZ, vx, vy, vz, lifetime);
    }

    /**
     * ticks all sectors with alive particles.
     */
    public void tick(float gravity) {
        for (RingBuffer ring : sectors.values()) {
            if (ring.aliveCount() > 0) {
                ring.tick(gravity);
            }
        }
    }

    /** performs batch block collision for all sectors with alive particles */
    public void batchCollideBlocks(RingBuffer.BlockCollisionTest test) {
        for (RingBuffer ring : sectors.values()) {
            if (ring.aliveCount() > 0) {
                ring.batchCollideBlocks(test);
            }
        }
    }

    /** returns total alive particles across all sectors */
    public int totalAlive() {
        int total = 0;
        for (RingBuffer ring : sectors.values()) {
            total += ring.aliveCount();
        }
        return total;
    }

    /** returns number of active sectors */
    public int sectorCount() {
        return sectors.size();
    }

    /** clears all particles but keeps sector buffers allocated */
    public void clearParticles() {
        for (RingBuffer ring : sectors.values()) {
            ring.clear();
        }
    }

    /** releases all sectors */
    public void clearAll() {
        sectors.clear();
    }
}
