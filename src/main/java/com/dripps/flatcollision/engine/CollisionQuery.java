package com.dripps.flatcollision.engine;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * collision query combining spatial grid and oversized entity list.
 * collects candidate slots from grid cells, performs fast AABB overlap tests
 * on SoA data, then fetches entities for final predicate checks.
 */
public final class CollisionQuery {

    private final SoAEntityData data;
    private final EntitySlotMap slotMap;
    private final SpatialGrid grid;
    private final OversizedEntityList oversized;

    /** scratch list to avoid per query allocation */
    private final IntArrayList candidateScratch = new IntArrayList(256);

    public CollisionQuery(SoAEntityData data, EntitySlotMap slotMap,
                          SpatialGrid grid, OversizedEntityList oversized) {
        this.data = data;
        this.slotMap = slotMap;
        this.grid = grid;
        this.oversized = oversized;
    }

    /**
     * returns entities overlapping box that match predicate.
     * replacement for vanilla getOtherEntities.
     */
    public List<Entity> getEntitiesInBox(@Nullable Entity except, Box box,
                                         Predicate<? super Entity> predicate) {
        List<Entity> result = new ArrayList<>();
        collectCandidates(box);

        int exceptId = except != null ? except.getId() : Integer.MIN_VALUE;

        double bMinX = box.minX, bMinY = box.minY, bMinZ = box.minZ;
        double bMaxX = box.maxX, bMaxY = box.maxY, bMaxZ = box.maxZ;

        int[] slots = candidateScratch.elements();
        int count = candidateScratch.size();

        for (int i = 0; i < count; i++) {
            int slot = slots[i];
            if (slot < 0 || slot >= slotMap.activeCount()) continue;

            // fast AABB overlap from SoA
            if (!data.overlapsBox(slot, bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ)) {
                continue;
            }

            // passed linear test, fetch entity for predicate checks
            Entity e = slotMap.getEntity(slot);
            if (e == null || e.getId() == exceptId) continue;
            if (!predicate.test(e)) continue;

            result.add(e);
        }

        return result;
    }

    /**
     * returns collision shapes for collidable entities overlapping box.
     * replacement for vanilla getEntityCollisions.
     */
    public List<VoxelShape> getEntityCollisionShapes(@Nullable Entity querier, Box box) {
        List<VoxelShape> shapes = new ArrayList<>();
        collectCandidates(box);

        int querierId = querier != null ? querier.getId() : Integer.MIN_VALUE;

        double bMinX = box.minX, bMinY = box.minY, bMinZ = box.minZ;
        double bMaxX = box.maxX, bMaxY = box.maxY, bMaxZ = box.maxZ;

        int[] slots = candidateScratch.elements();
        int count = candidateScratch.size();

        for (int i = 0; i < count; i++) {
            int slot = slots[i];
            if (slot < 0 || slot >= slotMap.activeCount()) continue;

            // fast AABB overlap from SoA
            if (!data.overlapsBox(slot, bMinX, bMinY, bMinZ, bMaxX, bMaxY, bMaxZ)) {
                continue;
            }

            Entity e = slotMap.getEntity(slot);
            if (e == null || e.getId() == querierId) continue;
            if (!e.isCollidable(querier)) continue;

            if (querier != null) {
                shapes.add(VoxelShapes.cuboid(e.getBoundingBox()));
            } else {
                shapes.add(VoxelShapes.cuboid(e.getBoundingBox()));
            }
        }

        return shapes;
    }

    /** populates scratch list with slot IDs from grid cells and oversized list */
    private void collectCandidates(Box box) {
        candidateScratch.clear();
        grid.collectSlotsInBox(box.minX, box.minZ, box.maxX, box.maxZ, candidateScratch);
        oversized.collectAll(candidateScratch);
    }
}
