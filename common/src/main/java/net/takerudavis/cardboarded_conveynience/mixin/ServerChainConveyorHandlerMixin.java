package net.takerudavis.cardboarded_conveynience.mixin;

import com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.takerudavis.cardboarded_conveynience.advancement.ModCriteria;
import net.takerudavis.cardboarded_conveynience.util.CardboardHelper;
import net.takerudavis.cardboarded_conveynience.util.SkyhookHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChainConveyorHandler.class)
public class ServerChainConveyorHandlerMixin {

    @Unique
    private static void cardboarded_conveynience$fixPose(Player player) {
        // Toggle pose to trigger dimension recalculation (fixes hitbox)
        Pose pose = player.getPose();
        player.setPose(pose == Pose.CROUCHING ? Pose.STANDING : Pose.CROUCHING);
        player.setPose(pose);
    }

    @Inject(method = "handleTTLPacket", at = @At("TAIL"), remap = false)
    private static void cardboarded_conveynience$injectedHandleTTLPacket(Player player, CallbackInfo ci) {
        cardboarded_conveynience$fixPose(player);

        // Award advancement when disguised on chain conveyor
        if (player instanceof ServerPlayer serverPlayer && CardboardHelper.testForArmor(player)) {
            var trigger = ModCriteria.getDisguiseTrigger();
            if (trigger != null) {
                trigger.trigger(serverPlayer);
            }
        }
    }

    @Inject(method = "handleStopRidingPacket", at = @At("TAIL"), remap = false)
    private static void cardboarded_conveynience$injectedHandleStopRidingPacket(Player player, CallbackInfo ci) {
        // Clear grace period immediately on dismount so stealth/hitbox updates
        SkyhookHelper.clearGracePeriod(player);
        cardboarded_conveynience$fixPose(player);
    }
}
