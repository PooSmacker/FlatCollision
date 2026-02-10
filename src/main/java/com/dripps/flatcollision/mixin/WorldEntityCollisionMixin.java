package com.dripps.flatcollision.mixin;

import com.dripps.flatcollision.engine.PhysicsEngine;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

/**
 * redirects vanilla entity spatial queries to flatcollision engine. targets
 * getOtherEntities and getEntityCollisions. only active for server worlds
 * with registered physics engine, otherwise falls through to vanilla.
 */
@Mixin(World.class)
public abstract class WorldEntityCollisionMixin {

    /** redirects getOtherEntities to spatial grid, vanilla iterates sections */
    @Inject(method = "getOtherEntities", at = @At("HEAD"), cancellable = true)
    private void flatcollision$redirectGetOtherEntities(
            @Nullable Entity except, Box box, Predicate<? super Entity> predicate,
            CallbackInfoReturnable<List<Entity>> cir) {

        World self = (World) (Object) this;
        if (!(self instanceof ServerWorld serverWorld)) return;

        PhysicsEngine engine = PhysicsEngine.get(serverWorld);
        if (engine == null || !engine.isActive()) return;

        cir.setReturnValue(engine.getEntitiesInBox(except, box, predicate));
    }
}
