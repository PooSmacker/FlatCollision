package com.dripps.flatcollision.engine;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * structure of arrays storage for entity physics data. stores all hot data in
 * contiguous off heap direct buffers to maximize cache prefetching. each field
 * is a separate array for tight iteration. zero allocation, grown by doubling.
 */
public final class SoAEntityData {

    private static final int DOUBLE_BYTES = Double.BYTES;
    private static final int INITIAL_CAPACITY = 1024;

    private int capacity;
    private int size;

    private ByteBuffer posX;
    private ByteBuffer posY;
    private ByteBuffer posZ;

    private ByteBuffer velX;
    private ByteBuffer velY;
    private ByteBuffer velZ;

    private ByteBuffer halfWidth;
    private ByteBuffer height;

    public SoAEntityData() {
        this(INITIAL_CAPACITY);
    }

    public SoAEntityData(int initialCapacity) {
        this.capacity = initialCapacity;
        this.size = 0;
        allocateBuffers(initialCapacity);
    }

    public int capacity() {
        return capacity;
    }

    public int size() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /** makking sure backing buffers can hold required slots, grows by doubling */
    public void ensureCapacity(int required) {
        if (required <= capacity) return;
        int newCap = Math.max(capacity * 2, required);
        grow(newCap);
    }

    public double getPosX(int slot) { return posX.getDouble(slot * DOUBLE_BYTES); }
    public double getPosY(int slot) { return posY.getDouble(slot * DOUBLE_BYTES); }
    public double getPosZ(int slot) { return posZ.getDouble(slot * DOUBLE_BYTES); }

    public void setPosition(int slot, double x, double y, double z) {
        int off = slot * DOUBLE_BYTES;
        posX.putDouble(off, x);
        posY.putDouble(off, y);
        posZ.putDouble(off, z);
    }

    public double getVelX(int slot) { return velX.getDouble(slot * DOUBLE_BYTES); }
    public double getVelY(int slot) { return velY.getDouble(slot * DOUBLE_BYTES); }
    public double getVelZ(int slot) { return velZ.getDouble(slot * DOUBLE_BYTES); }

    public void setVelocity(int slot, double vx, double vy, double vz) {
        int off = slot * DOUBLE_BYTES;
        velX.putDouble(off, vx);
        velY.putDouble(off, vy);
        velZ.putDouble(off, vz);
    }

    public double getHalfWidth(int slot) { return halfWidth.getDouble(slot * DOUBLE_BYTES); }
    public double getHeight(int slot)    { return height.getDouble(slot * DOUBLE_BYTES); }

    public void setDimensions(int slot, double hw, double h) {
        halfWidth.putDouble(slot * DOUBLE_BYTES, hw);
        height.putDouble(slot * DOUBLE_BYTES, h);
    }

    public double getMinX(int slot) { return getPosX(slot) - getHalfWidth(slot); }
    public double getMaxX(int slot) { return getPosX(slot) + getHalfWidth(slot); }
    public double getMinY(int slot) { return getPosY(slot); }
    public double getMaxY(int slot) { return getPosY(slot) + getHeight(slot); }
    public double getMinZ(int slot) { return getPosZ(slot) - getHalfWidth(slot); }
    public double getMaxZ(int slot) { return getPosZ(slot) + getHalfWidth(slot); }

    /** tests AABB overlap entirely from linear buffers, no entity dereference */
    public boolean overlapsBox(int slot, double boxMinX, double boxMinY, double boxMinZ,
                               double boxMaxX, double boxMaxY, double boxMaxZ) {
        double eMinX = getMinX(slot);
        double eMaxX = getMaxX(slot);
        if (eMaxX <= boxMinX || eMinX >= boxMaxX) return false;

        double eMinY = getMinY(slot);
        double eMaxY = getMaxY(slot);
        if (eMaxY <= boxMinY || eMinY >= boxMaxY) return false;

        double eMinZ = getMinZ(slot);
        double eMaxZ = getMaxZ(slot);
        return eMaxZ > boxMinZ && eMinZ < boxMaxZ;
    }

    /** copies all fields from src slot to dst slot for swap and pop */
    public void copySlot(int src, int dst) {
        int srcOff = src * DOUBLE_BYTES;
        int dstOff = dst * DOUBLE_BYTES;

        posX.putDouble(dstOff, posX.getDouble(srcOff));
        posY.putDouble(dstOff, posY.getDouble(srcOff));
        posZ.putDouble(dstOff, posZ.getDouble(srcOff));

        velX.putDouble(dstOff, velX.getDouble(srcOff));
        velY.putDouble(dstOff, velY.getDouble(srcOff));
        velZ.putDouble(dstOff, velZ.getDouble(srcOff));

        halfWidth.putDouble(dstOff, halfWidth.getDouble(srcOff));
        height.putDouble(dstOff, height.getDouble(srcOff));
    }

    private static ByteBuffer alloc(int slots) {
        return ByteBuffer.allocateDirect(slots * DOUBLE_BYTES)
                         .order(ByteOrder.nativeOrder());
    }

    private void allocateBuffers(int cap) {
        posX      = alloc(cap);
        posY      = alloc(cap);
        posZ      = alloc(cap);
        velX      = alloc(cap);
        velY      = alloc(cap);
        velZ      = alloc(cap);
        halfWidth = alloc(cap);
        height    = alloc(cap);
    }

    private void grow(int newCap) {
        posX      = copyGrow(posX, newCap);
        posY      = copyGrow(posY, newCap);
        posZ      = copyGrow(posZ, newCap);
        velX      = copyGrow(velX, newCap);
        velY      = copyGrow(velY, newCap);
        velZ      = copyGrow(velZ, newCap);
        halfWidth = copyGrow(halfWidth, newCap);
        height    = copyGrow(height, newCap);
        capacity  = newCap;
    }

    private static ByteBuffer copyGrow(ByteBuffer old, int newSlots) {
        ByteBuffer buf = alloc(newSlots);
        old.rewind();
        buf.put(old);
        buf.rewind();
        return buf;
    }

    /** releases off heap memory */
    public void free() {
        posX = posY = posZ = null;
        velX = velY = velZ = null;
        halfWidth = height = null;
        size = 0;
        capacity = 0;
    }
}
