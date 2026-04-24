package net.takerudavis.cardboarded_conveynience.mixin;

import com.simibubi.create.content.equipment.armor.CardboardArmorHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.takerudavis.cardboarded_conveynience.util.CardboardHelper;
import net.takerudavis.cardboarded_conveynience.util.SkyhookHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CardboardArmorHandler.class)
public class CardboardArmorHandlerMixin {

    @Inject(method = "testForStealth", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cardboarded_conveynience$injectedTestForStealth(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        if (!(entityIn instanceof LivingEntity entity)) {
            cir.setReturnValue(false);
            return;
        }

        if (entity instanceof Player player) {
            // Use isPlayerHanging (not grace) for hitbox calculations - matches original Forge behavior
            if (!SkyhookHelper.isPlayerHanging(player)) {
                if (entity.getPose() != Pose.CROUCHING) {
                    cir.setReturnValue(false);
                    return;
                }
                if (SkyhookHelper.isPlayerFlying(player)) {
                    cir.setReturnValue(false);
                    return;
                }
            }
            cir.setReturnValue(CardboardHelper.testForArmor(player));
        }
    }
}
