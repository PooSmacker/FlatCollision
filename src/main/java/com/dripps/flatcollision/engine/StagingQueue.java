package com.dripps.flatcollision.engine;

import net.minecraft.entity.Entity;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * thread safe staging queue for entities from async chunk loaders. async workers
 * push requests into lock free queue, main thread drains at tick start and bulk
 * inserts into SoA buffers.
 */
public final class StagingQueue {

    /** lightweight record for pending entity registration */
    public record EntityRequest(Entity entity, RequestType type) {}

    public enum RequestType {
        ADD,
        REMOVE
    }

    /** lock free queue, multiple threads enqueue, main thread dequeues */
    private final ConcurrentLinkedQueue<EntityRequest> pending = new ConcurrentLinkedQueue<>();

    /** enqueues entity to be added on next tick */
    public void enqueueAdd(Entity entity) {
        pending.add(new EntityRequest(entity, RequestType.ADD));
    }

    /** enqueues entity to be removed on next tick */
    public void enqueueRemove(Entity entity) {
        pending.add(new EntityRequest(entity, RequestType.REMOVE));
    }

    /** drains all pending requests and applies to physics engine */
    public int flush(PhysicsEngine engine) {
        int count = 0;
        EntityRequest req;
        while ((req = pending.poll()) != null) {
            switch (req.type()) {
                case ADD    -> engine.trackEntityDirect(req.entity());
                case REMOVE -> engine.untrackEntityDirect(req.entity());
            }
            count++;
        }
        return count;
    }

    /** returns true if pending requests exist */
    public boolean hasPending() {
        return !pending.isEmpty();
    }

    /** discards all pending requests */
    public void clear() {
        pending.clear();
    }
}
