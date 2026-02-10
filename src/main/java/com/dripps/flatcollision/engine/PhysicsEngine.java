package com.dripps.flatcollision.engine;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * per world physics engine instance. owns SoA data, slot map, spatial grid,
 * oversized list, and collision query. tick lifecycle flushes staging queue,
 * syncs positions, updates grid, then handles redirected collision queries.
 */
public final class PhysicsEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger("FlatCollision");

    private static final Map<ServerWorld, PhysicsEngine> ENGINES = new ConcurrentHashMap<>();

    public static @Nullable PhysicsEngine get(ServerWorld world) {
        return ENGINES.get(world);
    }

    public static PhysicsEngine getOrCreate(ServerWorld world) {
        return ENGINES.computeIfAbsent(world, w -> {
            LOGGER.info("[FlatCollision] physics engine created for this guy: {}", w.getRegistryKey().getValue());
            return new PhysicsEngine(w);
        });
    }

    public static void remove(ServerWorld world) {
        PhysicsEngine engine = ENGINES.remove(world);
        if (engine != null) {
            engine.shutdown();
            LOGGER.info("[FlatCollision] physics engine removed for this fella: {}", world.getRegistryKey().getValue());
        }
    }

    public static void removeAll() {
        ENGINES.values().forEach(PhysicsEngine::shutdown);
        ENGINES.clear();
    }

    private final ServerWorld world;
    private final SoAEntityData data;
    private final EntitySlotMap slotMap;
    private final SpatialGrid grid;
    private final OversizedEntityList oversized;
    private final CollisionQuery query;
    private final StagingQueue staging;

    /** tracks whether engine is active */
    private volatile boolean active = true;

    private PhysicsEngine(ServerWorld world) {
        this.world = world;
        this.data = new SoAEntityData();
        this.slotMap = new EntitySlotMap(data);
        this.grid = new SpatialGrid();
        this.oversized = new OversizedEntityList();
        this.query = new CollisionQuery(data, slotMap, grid, oversized);
        this.staging = new StagingQueue();
    }

    /**
     * called at start of each tick. drains staging queue, syncs entity positions
     * to SoA arrays, and updates spatial grid for moved entities.
     */
    public void onTickStart() {
        if (!active) return;

        staging.flush(this);

        int count = slotMap.activeCount();
        for (int slot = 0; slot < count; slot++) {
            Entity entity = slotMap.getEntity(slot);
            if (entity == null || !entity.isAlive()) {
                if (entity != null) staging.enqueueRemove(entity);
                continue;
            }

            double oldX = data.getPosX(slot);
            double oldZ = data.getPosZ(slot);

            slotMap.syncEntityToSlot(entity, slot);

            double newX = data.getPosX(slot);
            double newZ = data.getPosZ(slot);

            if (!OversizedEntityList.isOversized(entity.getWidth())) {
                grid.update(slot, oldX, oldZ, newX, newZ);
            }
        }
    }

    /** directly tracks entity, called from staging queue flush */
    public void trackEntityDirect(Entity entity) {
        if (!active) return;
        if (slotMap.isTracked(entity)) return;

        int slot = slotMap.allocate(entity);
        if (slot < 0) return;

        double width = entity.getWidth();
        if (OversizedEntityList.isOversized(width)) {
            oversized.add(slot);
        } else {
            grid.insert(slot, entity.getX(), entity.getZ());
        }
    }

    /** directly untracks entity, called from staging queue flush */
    public void untrackEntityDirect(Entity entity) {
        if (!active) return;
        int slot = slotMap.getSlot(entity);
        if (slot < 0) return;

        double width = entity.getWidth();
        double x = data.getPosX(slot);
        double z = data.getPosZ(slot);

        if (OversizedEntityList.isOversized(width)) {
            oversized.remove(slot);
        } else {
            grid.remove(slot, x, z);
        }

        int lastSlot = slotMap.activeCount() - 1;
        if (slot != lastSlot && lastSlot >= 0) {
            Entity movedEntity = slotMap.getEntity(lastSlot);
            if (movedEntity != null) {
                double movedX = data.getPosX(lastSlot);
                double movedZ = data.getPosZ(lastSlot);
                double movedWidth = movedEntity.getWidth();

                if (OversizedEntityList.isOversized(movedWidth)) {
                    oversized.remove(lastSlot);
                } else {
                    grid.remove(lastSlot, movedX, movedZ);
                }

                slotMap.free(entity);

                if (OversizedEntityList.isOversized(movedWidth)) {
                    oversized.add(slot);
                } else {
                    grid.insert(slot, movedX, movedZ);
                }
                return;
            }
        }

        slotMap.free(entity);
    }

    /** thread safe, enqueues entity to be added on next tick */
    public void trackEntity(Entity entity) {
        staging.enqueueAdd(entity);
    }

    /** thread safe, enqueues entity to be removed on next tick */
    public void untrackEntity(Entity entity) {
        staging.enqueueRemove(entity);
    }

    /** replacement for vanilla getOtherEntities using spatial grid */
    public List<Entity> getEntitiesInBox(@Nullable Entity except, Box box,
                                         Predicate<? super Entity> predicate) {
        return query.getEntitiesInBox(except, box, predicate);
    }

    /** replacement for vanilla getEntityCollisions */
    public List<VoxelShape> getEntityCollisionShapes(@Nullable Entity querier, Box box) {
        return query.getEntityCollisionShapes(querier, box);
    }

    public int trackedEntityCount() {
        return slotMap.activeCount();
    }

    public int gridCellCount() {
        return grid.cellCount();
    }

    public int oversizedCount() {
        return oversized.size();
    }

    public boolean isActive() {
        return active;
    }

    private void shutdown() {
        active = false;
        staging.clear();
        slotMap.clear();
        grid.clear();
        oversized.clear();
        data.free();
    }
}
