package com.dripps.flatcollision.mixin;

import com.dripps.flatcollision.engine.PhysicsEngine;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * intercepts getEntityCollisions at interface level. hot path for entity
 * movement collision, covers all implementors without requiring overrides.
 */
@Mixin(EntityView.class)
public interface EntityViewCollisionMixin {

    @Inject(method = "getEntityCollisions", at = @At("HEAD"), cancellable = true)
    default void flatcollision$redirectGetEntityCollisions(
            Entity entity, Box box,
            CallbackInfoReturnable<List<VoxelShape>> cir) {

        if (this instanceof ServerWorld serverWorld) {
            PhysicsEngine engine = PhysicsEngine.get(serverWorld);
            if (engine != null && engine.isActive()) {
                cir.setReturnValue(engine.getEntityCollisionShapes(entity, box));
            }
        }
    }
}
