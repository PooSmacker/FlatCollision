package com.dripps.flatcollision;

import com.dripps.flatcollision.engine.PhysicsEngine;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * server side entry point. registers hooks to sync per world {@link PhysicsEngine}
 * instances with vanilla lifecycle events (world load, entity load, tick start).
 */
public class Flatcollision implements ModInitializer {

    public static final String MOD_ID = "flatcollision";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[FlatCollision] starting cache aware physics optimization and drinking matcha ew");

        ServerWorldEvents.LOAD.register((server, world) -> {
            PhysicsEngine.getOrCreate(world);
        });

        ServerWorldEvents.UNLOAD.register((server, world) -> {
            PhysicsEngine.remove(world);
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            PhysicsEngine engine = PhysicsEngine.get(world);
            if (engine != null) {
                engine.trackEntity(entity);
            }
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            PhysicsEngine engine = PhysicsEngine.get(world);
            if (engine != null) {
                engine.untrackEntity(entity);
            }
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (var world : server.getWorlds()) {
                PhysicsEngine engine = PhysicsEngine.get(world);
                if (engine != null) {
                    engine.onTickStart();
                }
            }
        });

        LOGGER.info("[FlatCollision] server side hooks registered :D");
    }
}
