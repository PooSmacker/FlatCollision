package com.dripps.flatcollision.client.particle;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * fire and forget particle engine using ring buffers. particles are stateless
 * visual noise with zero GC (ring buffers overwrite old data), batch collision
 * checks, and sector based spatial grouping.
 */
public final class FlatParticleEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger("FlatCollision/Particles");

    /** default particle gravity in blocks per tick squared */
    private static final float DEFAULT_GRAVITY = -0.04f;

    /** default particle lifetime in ticks */
    private static final int DEFAULT_LIFETIME = 40;

    public static final FlatParticleEngine INSTANCE = new FlatParticleEngine();

    private FlatParticleEngine() {}

    private final SectorGrid sectorGrid = new SectorGrid();
    private boolean initialized = false;

    public void initialize() {
        if (initialized) return;
        initialized = true;
        LOGGER.info("[FlatCollision] particle engine started sir (sectors={}, ringCap={})",
                SectorGrid.SECTOR_SIZE, 2048);
    }

    public void shutdown() {
        sectorGrid.clearAll();
        initialized = false;
    }

    /**
     * adds particle to ring buffer. called alongside vanilla particle manager
     * for collision tracking.
     */
    public void addParticle(double x, double y, double z,
                            double vx, double vy, double vz) {
        addParticle(x, y, z, vx, vy, vz, DEFAULT_LIFETIME);
    }

    public void addParticle(double x, double y, double z,
                            double vx, double vy, double vz,
                            int lifetime) {
        if (!initialized) return;
        sectorGrid.emit(x, y, z, (float) vx, (float) vy, (float) vz, lifetime);
    }

    /**
     * called once per client tick. advances particle positions and performs
     * batch block collision.
     */
    public void tick() {
        if (!initialized) return;

        ClientWorld world = getClientWorld();

        sectorGrid.tick(DEFAULT_GRAVITY);

        if (world != null) {
            final ClientWorld w = world;
            sectorGrid.batchCollideBlocks((x, y, z) -> {
                BlockPos pos = BlockPos.ofFloored(x, y, z);
                BlockState state = w.getBlockState(pos);
                return !state.isAir() && state.isFullCube(w, pos);
            });
        }
    }

    public int totalAliveParticles() {
        return sectorGrid.totalAlive();
    }

    public int sectorCount() {
        return sectorGrid.sectorCount();
    }

    @Nullable
    private static ClientWorld getClientWorld() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null ? client.world : null;
    }
}
