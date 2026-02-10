package com.dripps.flatcollision.client;

import com.dripps.flatcollision.client.particle.FlatParticleEngine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * client side entry point. initializes ring buffer particle engine
 * and registers cleanup hooks.
 */
public class FlatcollisionClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("FlatCollision/Client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[FlatCollision] starting client side particle engine");

        FlatParticleEngine.INSTANCE.initialize();

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.world == null) {
                FlatParticleEngine.INSTANCE.shutdown();
            }
        });

        LOGGER.info("[FlatCollision] client side particle engine initialized and hooks registered and stuff and things and whatnot and all that good jazz and also some more words to make this log message longer and more descriptive and informative and maybe even a little bit entertaining too");
    }
}
