package net.TakeruDavis.create_cardboarded_conveynience.mixin;

import com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChainConveyorHandler.class)
public class ServerChainConveyorHandlerMixin {

    @Unique
    private static void create_cardboarded_conveynience$fixPose(Player player) {
        Pose pose = player.getPose();
        player.setPose(pose == Pose.CROUCHING ? Pose.STANDING : Pose.CROUCHING);
        player.setPose(pose);
    }

    @Inject(method = "handleTTLPacket", at = @At("TAIL"), remap = false)
    private static void injectedHandleTTLPacket(Player player, CallbackInfo ci) {
        create_cardboarded_conveynience$fixPose(player);
    }

    @Inject(method = "handleStopRidingPacket", at = @At("TAIL"), remap = false)
    private static void injectedHandleStopRidingPacket(Player player, CallbackInfo ci) {
        create_cardboarded_conveynience$fixPose(player);
    }

}
