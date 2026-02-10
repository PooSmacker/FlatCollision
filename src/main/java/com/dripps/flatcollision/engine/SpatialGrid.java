package com.dripps.flatcollision.engine;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * flat spatial grid for entity collision lookups. entities assigned to single
 * home cell based on center point. collision queries check home cell and 8
 * neighbors. cell size is 16 blocks, chunk aligned.
 */
public final class SpatialGrid {

    /** cell size in blocks, chunk aligned */
    public static final int CELL_SIZE = 16;

    /** map from packed cell key to list of slot IDs */
    private final Long2ObjectOpenHashMap<IntArrayList> cells = new Long2ObjectOpenHashMap<>();

    /** converts world coordinate to cell coordinate */
    public static int toCellCoord(double worldCoord) {
        return Math.floorDiv((int) Math.floor(worldCoord), CELL_SIZE);
    }

    /** packs two cell coordinates into long key */
    public static long packKey(int cellX, int cellZ) {
        return ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL);
    }

    /** inserts slot into cell at world position, returns packed key */
    public long insert(int slot, double worldX, double worldZ) {
        int cx = toCellCoord(worldX);
        int cz = toCellCoord(worldZ);
        long key = packKey(cx, cz);
        cells.computeIfAbsent(key, k -> new IntArrayList()).add(slot);
        return key;
    }

    /** removes slot from cell at world position */
    public void remove(int slot, double worldX, double worldZ) {
        int cx = toCellCoord(worldX);
        int cz = toCellCoord(worldZ);
        long key = packKey(cx, cz);
        IntArrayList list = cells.get(key);
        if (list != null) {
            list.rem(slot); // IntArrayList.rem removes by value
            if (list.isEmpty()) {
                cells.remove(key);
            }
        }
    }

    /** removes from old cell and inserts into new, only if cell changed */
    public long update(int slot, double oldX, double oldZ, double newX, double newZ) {
        int oldCx = toCellCoord(oldX);
        int oldCz = toCellCoord(oldZ);
        int newCx = toCellCoord(newX);
        int newCz = toCellCoord(newZ);

        if (oldCx == newCx && oldCz == newCz) {
            return packKey(newCx, newCz);
        }

        // Remove from old cell
        long oldKey = packKey(oldCx, oldCz);
        IntArrayList oldList = cells.get(oldKey);
        if (oldList != null) {
            oldList.rem(slot);
            if (oldList.isEmpty()) cells.remove(oldKey);
        }

        long newKey = packKey(newCx, newCz);
        cells.computeIfAbsent(newKey, k -> new IntArrayList()).add(slot);
        return newKey;
    }

    /** returns slot list for cell or null if empty */
    public IntArrayList getCell(int cellX, int cellZ) {
        return cells.get(packKey(cellX, cellZ));
    }

    /** collects all slots from 3x3 neighborhood into output list */
    public void collectNeighborSlots(int centerCellX, int centerCellZ, IntArrayList out) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                IntArrayList list = cells.get(packKey(centerCellX + dx, centerCellZ + dz));
                if (list != null) {
                    out.addAll(list);
                }
            }
        }
    }

    /** collects slots from all cells overlapping AABB */
    public void collectSlotsInBox(double minX, double minZ, double maxX, double maxZ,
                                  IntArrayList out) {
        int cellMinX = toCellCoord(minX) - 1;
        int cellMaxX = toCellCoord(maxX) + 1;
        int cellMinZ = toCellCoord(minZ) - 1;
        int cellMaxZ = toCellCoord(maxZ) + 1;

        for (int cx = cellMinX; cx <= cellMaxX; cx++) {
            for (int cz = cellMinZ; cz <= cellMaxZ; cz++) {
                IntArrayList list = cells.get(packKey(cx, cz));
                if (list != null) {
                    out.addAll(list);
                }
            }
        }
    }

    /** removes all entries */
    public void clear() {
        cells.clear();
    }

    public int cellCount() {
        return cells.size();
    }
}
