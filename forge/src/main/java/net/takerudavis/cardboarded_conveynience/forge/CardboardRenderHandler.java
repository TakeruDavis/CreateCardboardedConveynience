package net.takerudavis.cardboarded_conveynience.forge;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;
import net.takerudavis.cardboarded_conveynience.util.CardboardRenderUtil;

@Mod.EventBusSubscriber(modid = CardboardedConveynience.MOD_ID, value = Dist.CLIENT)
public class CardboardRenderHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (CardboardRenderUtil.renderCardboardPlayer(
                player,
                event.getPartialTick(),
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight()
        )) {
            event.setCanceled(true);
        }
    }
}
