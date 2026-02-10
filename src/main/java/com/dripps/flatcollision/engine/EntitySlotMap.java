package com.dripps.flatcollision.engine;

import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * bidirectional mapping between entity IDs and dense SoA slot indices.
 * uses swap and pop strategy to keep active region packed with no gaps.
 */
public final class EntitySlotMap {

    private static final int INITIAL_CAPACITY = 1024;

    /** sparse entity id to slot index map */
    private final Map<Integer, Integer> entityIdToSlot = new HashMap<>();

    /** dense slot index to entity reference array */
    private Entity[] slotToEntity;

    /** number of active slots */
    private int activeCount;

    private final SoAEntityData data;

    public EntitySlotMap(SoAEntityData data) {
        this.data = data;
        this.slotToEntity = new Entity[INITIAL_CAPACITY];
        this.activeCount = 0;
    }

    public int activeCount() {
        return activeCount;
    }

    /** returns slot index for entity or -1 if not tracked */
    public int getSlot(Entity entity) {
        Integer slot = entityIdToSlot.get(entity.getId());
        return slot != null ? slot : -1;
    }

    /** returns slot index for entity id or -1 if not tracked */
    public int getSlot(int entityId) {
        Integer slot = entityIdToSlot.get(entityId);
        return slot != null ? slot : -1;
    }

    /** returns entity reference at slot */
    @Nullable
    public Entity getEntity(int slot) {
        if (slot < 0 || slot >= activeCount) return null;
        return slotToEntity[slot];
    }

    public boolean isTracked(Entity entity) {
        return entityIdToSlot.containsKey(entity.getId());
    }

    /**
     * allocates new slot for entity and writes initial physics data.
     * returns allocated slot index or -1 if already tracked.
     */
    public int allocate(Entity entity) {
        int id = entity.getId();
        if (entityIdToSlot.containsKey(id)) return -1;

        int slot = activeCount;
        ensureSlotCapacity(slot + 1);

        entityIdToSlot.put(id, slot);
        slotToEntity[slot] = entity;
        activeCount++;

        syncEntityToSlot(entity, slot);
        data.setSize(activeCount);
        return slot;
    }

    /**
     * removes entity using swap and pop. last active entity data moves into
     * vacated slot. returns freed slot index or -1 if not tracked.
     */
    public int free(Entity entity) {
        Integer removedSlotObj = entityIdToSlot.remove(entity.getId());
        if (removedSlotObj == null) return -1;

        int removedSlot = removedSlotObj;
        int lastSlot = activeCount - 1;

        if (removedSlot != lastSlot) {
            Entity lastEntity = slotToEntity[lastSlot];
            slotToEntity[removedSlot] = lastEntity;
            entityIdToSlot.put(lastEntity.getId(), removedSlot);

            data.copySlot(lastSlot, removedSlot);
        }

        slotToEntity[lastSlot] = null;
        activeCount--;
        data.setSize(activeCount);
        return removedSlot;
    }

    /** syncs entity state into SoA slot */
    public void syncEntityToSlot(Entity entity, int slot) {
        data.setPosition(slot, entity.getX(), entity.getY(), entity.getZ());
        net.minecraft.util.math.Vec3d vel = entity.getVelocity();
        data.setVelocity(slot, vel.x, vel.y, vel.z);
        data.setDimensions(slot, entity.getWidth() / 2.0, entity.getHeight());
    }

    /** bulk syncs all tracked entity positions into SoA arrays */
    public void syncAll() {
        for (int i = 0; i < activeCount; i++) {
            Entity e = slotToEntity[i];
            if (e != null && e.isAlive()) {
                syncEntityToSlot(e, i);
            }
        }
    }

    private void ensureSlotCapacity(int required) {
        data.ensureCapacity(required);
        if (required > slotToEntity.length) {
            int newLen = Math.max(slotToEntity.length * 2, required);
            Entity[] grown = new Entity[newLen];
            System.arraycopy(slotToEntity, 0, grown, 0, activeCount);
            slotToEntity = grown;
        }
    }

    /** releases all references */
    public void clear() {
        entityIdToSlot.clear();
        for (int i = 0; i < activeCount; i++) {
            slotToEntity[i] = null;
        }
        activeCount = 0;
        data.setSize(0);
    }
}
