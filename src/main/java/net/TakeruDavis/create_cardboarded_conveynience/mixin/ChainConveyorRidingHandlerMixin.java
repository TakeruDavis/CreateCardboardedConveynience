package net.TakeruDavis.create_cardboarded_conveynience.mixin;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler;
import net.TakeruDavis.create_cardboarded_conveynience.utils.CardboardHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChainConveyorRidingHandler.class)
public class ChainConveyorRidingHandlerMixin {

    @ModifyVariable(
        method = "clientTick",
        at = @At(
            value = "STORE"
        ),
        name = "targetPosition",
        remap = false
    )
    private static Vec3 adjustTargetPosition(Vec3 original) {
        Minecraft mc = Minecraft.getInstance();
        if (CardboardHelper.testForArmor(mc.player)) {
            return original.add(0, 0.25, 0);
        }
        return original;
    }

}
