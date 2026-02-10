package com.dripps.flatcollision.mixin.client;

import com.dripps.flatcollision.client.particle.FlatParticleEngine;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * hooks into vanilla particle manager to mirror particle data into ring buffer
 * system. injects alongside vanilla rather than replacing it. vanilla handles
 * rendering, our system provides batched physics and collision.
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    /** mirrors new particles into ring buffer, vanilla particle still created */
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"))
    private void flatcollision$onAddParticle(
            ParticleEffect parameters,
            double x, double y, double z,
            double velocityX, double velocityY, double velocityZ,
            CallbackInfoReturnable<Particle> cir) {

        FlatParticleEngine.INSTANCE.addParticle(x, y, z, velocityX, velocityY, velocityZ);
    }

    /** runs batch tick before vanilla particle tick */
    @Inject(method = "tick", at = @At("HEAD"))
    private void flatcollision$onTick(CallbackInfo ci) {
        FlatParticleEngine.INSTANCE.tick();
    }
}
