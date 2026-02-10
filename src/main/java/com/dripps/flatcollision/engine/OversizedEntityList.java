package com.dripps.flatcollision.engine;

import it.unimi.dsi.fastutil.ints.IntArrayList;

/**
 * global list for entities exceeding grid cell size. collision queries check
 * both local grid and this list. stays small since oversized entities are rare.
 */
public final class OversizedEntityList {

    /** entities wider than this threshold go in global list instead of grid */
    public static final double OVERSIZED_THRESHOLD = SpatialGrid.CELL_SIZE * 0.75;

    /** dense list of slot IDs for oversized entities */
    private final IntArrayList slots = new IntArrayList();

    public void add(int slot) {
        if (!slots.contains(slot)) {
            slots.add(slot);
        }
    }

    public void remove(int slot) {
        slots.rem(slot);
    }

    /** appends all slot ids to output list without clearing */
    public void collectAll(IntArrayList out) {
        out.addAll(slots);
    }

    public int size() {
        return slots.size();
    }

    /** returns true if entity width exceeds threshold */
    public static boolean isOversized(double entityWidth) {
        return entityWidth > OVERSIZED_THRESHOLD;
    }

    public void clear() {
        slots.clear();
    }
}
