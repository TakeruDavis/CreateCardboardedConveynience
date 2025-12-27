package net.takerudavis.cardboarded_conveynience.fabric.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side mixin to ensure hitbox/dimensions are recalculated when pose syncs from server.
 * Fixes issue where player dismounts chain conveyor but hitbox stays stuck in crouching size.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDimensionSyncMixin {

    @Inject(method = "onSyncedDataUpdated", at = @At("TAIL"))
    private void cardboarded_conveynience$refreshDimensionsOnPoseChange(EntityDataAccessor<?> key, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Only act on client side, for players, when pose data changes
        if (self instanceof Player && self.level().isClientSide() && key == EntityAccessor.getDataPose()) {
            self.refreshDimensions();
        }
    }
}
