package net.takerudavis.cardboarded_conveynience.mixin;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.takerudavis.cardboarded_conveynience.util.CardboardHelper;
import net.takerudavis.cardboarded_conveynience.util.ModConstants;
import net.takerudavis.cardboarded_conveynience.util.SkyhookHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChainConveyorRidingHandler.class)
public class ChainConveyorRidingHandlerMixin {

    @ModifyVariable(
        method = "clientTick",
        at = @At(value = "STORE"),
        name = "targetPosition",
        remap = false
    )
    private static Vec3 cardboarded_conveynience$adjustTargetPosition(Vec3 original) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && CardboardHelper.testForArmor(mc.player)) {
            double offset = ModConstants.CHAIN_POSITION_OFFSET;
            if (SkyhookHelper.isPlayerFlying(mc.player)) {
                offset += ModConstants.CHAIN_POSITION_FLYING_OFFSET;
            }
            return original.add(0.0D, offset, 0.0D);
        }
        return original;
    }
}
