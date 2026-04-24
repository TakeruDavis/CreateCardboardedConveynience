package net.takerudavis.cardboarded_conveynience.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.box.PackageRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;

import java.util.concurrent.ExecutionException;

/**
 * Shared rendering logic for cardboard box disguise with wrench.
 * Used by both Fabric (mixin) and Forge (event handler).
 */
public class CardboardRenderUtil {

    /**
     * Renders the player as a cardboard box with optional wrench when hanging.
     * Returns true if rendering was handled (caller should cancel normal rendering).
     */
    public static boolean renderCardboardPlayer(
            Player player,
            float partialTicks,
            PoseStack ms,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        if (!CardboardHelper.testForStealthExtended(player)) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            return true; // Cancel rendering but don't render box in first person
        }

        ms.pushPose();
        ms.translate(0.0F, ModConstants.BOX_RENDER_OFFSET, 0.0F);

        float movement = (float) player.position().subtract(player.xo, player.yo, player.zo).length();
        if (player.onGround()) {
            ms.translate(0.0F, Math.min(Math.abs(Mth.cos(AnimationTickHolder.getRenderTime() % 256.0F / 2.0F)) * 2.0F / 16.0F, movement * 5.0F), 0.0F);
        }

        float interpolatedYaw = Mth.lerp(partialTicks, player.yRotO, player.getYRot());

        try {
            PartialModel model = AllPartialModels.PACKAGES_TO_HIDE_AS.get(CardboardHelper.getCurrentBoxIndex(player));

            if (SkyhookHelper.isPlayerHangingWithGrace(player)) {
                // Position offset for hanging - flying adjustment handled by ChainConveyorRidingHandlerMixin
                ms.translate(0, ModConstants.WRENCH_RENDER_OFFSET, 0);

                // Calculate travel direction for wrench rotation
                double deltaX = player.getX() - player.xo;
                double deltaZ = player.getZ() - player.zo;
                boolean isMoving = deltaX != 0 || deltaZ != 0;
                float travelYaw = isMoving ? (float) Math.toDegrees(Math.atan2(deltaX, deltaZ)) : 0;

                if (isMoving) {
                    ms.mulPose(Axis.YP.rotationDegrees(travelYaw));
                }

                // Render wrench
                ItemRenderer itemRenderer = mc.getItemRenderer();
                itemRenderer.renderStatic(
                    player.getMainHandItem(),
                    ItemDisplayContext.FIXED,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    ms,
                    bufferSource,
                    player.level(),
                    0
                );

                // Reset rotation for package (it uses interpolatedYaw)
                if (isMoving) {
                    ms.mulPose(Axis.YP.rotationDegrees(-travelYaw));
                }

                // Render package closer to the chain
                ms.translate(0, ModConstants.PACKAGE_RENDER_OFFSET, 0);
            }

            PackageRenderer.renderBox(player, interpolatedYaw, ms, bufferSource, packedLight, model);
        } catch (ExecutionException e) {
            CardboardedConveynience.LOGGER.error("Failed to get cardboard box index for player", e);
        }

        ms.popPose();
        return true;
    }
}
