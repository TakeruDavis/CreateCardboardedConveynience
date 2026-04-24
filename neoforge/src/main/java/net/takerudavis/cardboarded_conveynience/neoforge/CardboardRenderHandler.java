package net.takerudavis.cardboarded_conveynience.neoforge;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;
import net.takerudavis.cardboarded_conveynience.util.CardboardRenderUtil;

@EventBusSubscriber(modid = CardboardedConveynience.MOD_ID, value = Dist.CLIENT)
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
