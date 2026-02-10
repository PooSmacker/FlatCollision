package com.dripps.flatcollision.client.particle;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * fixed size circular buffer for particle data. stores particle state in
 * contiguous off heap memory using structure of arrays layout. when full,
 * new particles overwrite oldest entry with zero gc pressure.
 */
public final class RingBuffer {

    private static final int FLOAT_BYTES = Float.BYTES;
    private static final int INT_BYTES   = Integer.BYTES;

    private final int capacity;

    private final ByteBuffer posX, posY, posZ;
    private final ByteBuffer velX, velY, velZ;

    private final ByteBuffer age;
    private final ByteBuffer maxAge;

    private final ByteBuffer alive;

    /** write head, wraps at capacity */
    private int head;

    /** currently alive particle count */
    private int aliveCount;

    public RingBuffer(int capacity) {
        this.capacity = capacity;
        this.head = 0;
        this.aliveCount = 0;

        posX = allocFloat(capacity);
        posY = allocFloat(capacity);
        posZ = allocFloat(capacity);
        velX = allocFloat(capacity);
        velY = allocFloat(capacity);
        velZ = allocFloat(capacity);
        age    = allocInt(capacity);
        maxAge = allocInt(capacity);
        alive  = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    public int capacity()   { return capacity; }
    public int aliveCount() { return aliveCount; }

    /**
     * writes new particle at ring head. if full, overwrites oldest.
     */
    public int emit(float px, float py, float pz,
                    float vx, float vy, float vz,
                    int lifetime) {
        int slot = head;

        if (alive.get(slot) == 1) {
            aliveCount--;
        }

        int fo = slot * FLOAT_BYTES;
        posX.putFloat(fo, px);
        posY.putFloat(fo, py);
        posZ.putFloat(fo, pz);
        velX.putFloat(fo, vx);
        velY.putFloat(fo, vy);
        velZ.putFloat(fo, vz);

        int io = slot * INT_BYTES;
        age.putInt(io, 0);
        maxAge.putInt(io, lifetime);
        alive.put(slot, (byte) 1);

        aliveCount++;
        head = (head + 1) % capacity;
        return slot;
    }

    /**
     * advances all alive particles by one tick. integrates velocity, applies
     * gravity, increments age, and kills expired particles. tight linear scan.
     */
    public void tick(float gravity) {
        for (int i = 0; i < capacity; i++) {
            if (alive.get(i) == 0) continue;

            int fo = i * FLOAT_BYTES;
            int io = i * INT_BYTES;

            float vy = velY.getFloat(fo) + gravity;
            velY.putFloat(fo, vy);

            posX.putFloat(fo, posX.getFloat(fo) + velX.getFloat(fo));
            posY.putFloat(fo, posY.getFloat(fo) + vy);
            posZ.putFloat(fo, posZ.getFloat(fo) + velZ.getFloat(fo));

            int currentAge = age.getInt(io) + 1;
            age.putInt(io, currentAge);

            if (currentAge >= maxAge.getInt(io)) {
                alive.put(i, (byte) 0);
                aliveCount--;
            }
        }
    }

    public boolean isAlive(int slot) { return alive.get(slot) == 1; }

    public float getPosX(int slot) { return posX.getFloat(slot * FLOAT_BYTES); }
    public float getPosY(int slot) { return posY.getFloat(slot * FLOAT_BYTES); }
    public float getPosZ(int slot) { return posZ.getFloat(slot * FLOAT_BYTES); }

    public float getVelX(int slot) { return velX.getFloat(slot * FLOAT_BYTES); }
    public float getVelY(int slot) { return velY.getFloat(slot * FLOAT_BYTES); }
    public float getVelZ(int slot) { return velZ.getFloat(slot * FLOAT_BYTES); }

    public int getAge(int slot)    { return age.getInt(slot * INT_BYTES); }
    public int getMaxAge(int slot) { return maxAge.getInt(slot * INT_BYTES); }

    /**
     * performs batch block collision for all alive particles. checks predicted
     * next position against block grid and zeros velocity on collision.
     */
    public void batchCollideBlocks(BlockCollisionTest test) {
        for (int i = 0; i < capacity; i++) {
            if (alive.get(i) == 0) continue;

            int fo = i * FLOAT_BYTES;
            float px = posX.getFloat(fo);
            float py = posY.getFloat(fo);
            float pz = posZ.getFloat(fo);

            float nx = px + velX.getFloat(fo);
            float ny = py + velY.getFloat(fo);
            float nz = pz + velZ.getFloat(fo);

            if (test.isSolid(nx, ny, nz)) {
                velX.putFloat(fo, 0f);
                velY.putFloat(fo, 0f);
                velZ.putFloat(fo, 0f);
            }
        }
    }

    /** functional interface for block solidity tests */
    @FunctionalInterface
    public interface BlockCollisionTest {
        boolean isSolid(float x, float y, float z);
    }

    /** kills all particles without releasing buffers */
    public void clear() {
        for (int i = 0; i < capacity; i++) {
            alive.put(i, (byte) 0);
        }
        aliveCount = 0;
        head = 0;
    }

    private static ByteBuffer allocFloat(int slots) {
        return ByteBuffer.allocateDirect(slots * FLOAT_BYTES).order(ByteOrder.nativeOrder());
    }

    private static ByteBuffer allocInt(int slots) {
        return ByteBuffer.allocateDirect(slots * INT_BYTES).order(ByteOrder.nativeOrder());
    }
}
