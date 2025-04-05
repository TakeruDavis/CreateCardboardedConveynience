package net.TakeruDavis.create_cardboarded_conveynience;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = CreateCardboardedConveynience.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Boolean> HOTSWAP_FIX = BUILDER
            .comment("Enable fix for carboard armor hotswap issue. This is a workaround for a bug in Create. If Create fixes it, disable this.")
            .define("hotswapFix", true);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static Boolean hotswapFix;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        hotswapFix = HOTSWAP_FIX.get();
    }
}
