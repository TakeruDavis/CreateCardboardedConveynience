package net.takerudavis.cardboarded_conveynience.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.armor.CardboardArmorHandlerClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.player.Player;
import net.takerudavis.cardboarded_conveynience.util.CardboardHelper;
import net.takerudavis.cardboarded_conveynience.util.CardboardRenderUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin into Create's CardboardArmorHandlerClient to:
 * 1. Add wrench rendering when hanging
 * 2. Use grace period for rendering (prevents flicker) while hitbox uses no grace
 */
@Mixin(value = CardboardArmorHandlerClient.class, remap = false)
public class CardboardArmorHandlerClientMixin {

    @Inject(
        method = "playerRendersAsBoxWhenSneaking",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void cardboarded_conveynience$handleRenderingWithGrace(
            Player player,
            PlayerRenderer renderer,
            float partialTicks,
            PoseStack ms,
            MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Use grace-period-aware stealth check for rendering
        // This prevents visual flicker while hitbox updates immediately
        if (CardboardHelper.testForStealthExtended(player)) {
            // We're in stealth (either actually hanging, in grace period, or crouching)
            // Render the box ourselves with wrench support
            boolean rendered = CardboardRenderUtil.renderCardboardPlayer(
                player, partialTicks, ms, bufferSource, packedLight
            );
            cir.setReturnValue(rendered);
        } else {
            // Not in stealth - don't render as box
            cir.setReturnValue(false);
        }
    }
}
