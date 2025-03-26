package net.TakeruDavis.create_cardboarded_conveynience.utils;

import com.google.common.cache.Cache;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.equipment.armor.CardboardArmorHandler;
import com.simibubi.create.content.equipment.armor.CardboardArmorHandlerClient;
import com.simibubi.create.content.logistics.box.PackageRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class CardboardHelper {

    private static Cache<UUID, Integer> BOXES_PLAYERS_ARE_HIDING_AS;

    private static void initLinked() {
        try {
            Field f = CardboardArmorHandlerClient.class.getDeclaredField("BOXES_PLAYERS_ARE_HIDING_AS");
            f.setAccessible(true);
            BOXES_PLAYERS_ARE_HIDING_AS = (Cache<UUID, Integer>) f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static boolean testForArmor(LivingEntity entity) {
        if (!AllItems.CARDBOARD_HELMET.isIn(entity.getItemBySlot(EquipmentSlot.HEAD)))
            return false;
        if (!AllItems.CARDBOARD_CHESTPLATE.isIn(entity.getItemBySlot(EquipmentSlot.CHEST)))
            return false;
        if (!AllItems.CARDBOARD_LEGGINGS.isIn(entity.getItemBySlot(EquipmentSlot.LEGS)))
            return false;
        if (!AllItems.CARDBOARD_BOOTS.isIn(entity.getItemBySlot(EquipmentSlot.FEET)))
            return false;
        return true;
    }

    public static Integer getCurrentBoxIndex(Player player) throws ExecutionException {
        if (BOXES_PLAYERS_ARE_HIDING_AS == null) {
            initLinked();
        }

        return BOXES_PLAYERS_ARE_HIDING_AS.get(player.getUUID(),
                () -> player.level().random.nextInt(AllPartialModels.PACKAGES_TO_HIDE_AS.size()));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void playerRendersAsBoxWhenSneaking(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (CardboardArmorHandler.testForStealth(player)) {
            event.setCanceled(true);
            if (player != Minecraft.getInstance().player || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) {
                PoseStack ms = event.getPoseStack();
                ms.pushPose();
                ms.translate(0.0F, 0.125F, 0.0F);
                float movement = (float)player.position().subtract(player.xo, player.yo, player.zo).length();
                if (player.onGround()) {
                    ms.translate(0.0F, Math.min(Math.abs(Mth.cos(AnimationTickHolder.getRenderTime() % 256.0F / 2.0F)) * 2.0F / 16.0F, movement * 5.0F), 0.0F);
                }

                float interpolatedYaw = Mth.lerp(event.getPartialTick(), player.yRotO, player.getYRot());

                try {
                    PartialModel model = AllPartialModels.PACKAGES_TO_HIDE_AS.get(getCurrentBoxIndex(player));

                    if (SkyhookHelper.isPlayerHanging(player)) {
                        //flying makes the player render higher, so we need to adjust the box
                        ms.translate(0, player.getAbilities().flying ? 0.5 : 0.75, 0);

                        //render wrench
                        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
                        itemRenderer.renderStatic(
                            player.getMainHandItem(),
                            ItemDisplayContext.FIXED,
                            event.getPackedLight(),
                            OverlayTexture.NO_OVERLAY,
                            ms,
                            event.getMultiBufferSource(),
                            player.level(),
                            0
                        );

                        //render package closer to the chain
                        ms.translate(0, -0.9, 0);
                    }

                    PackageRenderer.renderBox(player, interpolatedYaw, ms, event.getMultiBufferSource(), event.getPackedLight(), model);
                } catch (ExecutionException var6) {
                    var6.printStackTrace();
                }

                ms.popPose();
            }
        }
    }

}
