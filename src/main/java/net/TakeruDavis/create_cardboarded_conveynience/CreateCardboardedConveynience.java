package net.TakeruDavis.create_cardboarded_conveynience;

import mod.TakeruDavis.create_cardboarded_conveynience.BuildConfig;
import net.TakeruDavis.create_cardboarded_conveynience.register.config.ModConfigs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BuildConfig.MODID)
public class CreateCardboardedConveynience {
	// Directly reference a log4j logger.
	public static IEventBus modEventBus;

	public CreateCardboardedConveynience() {
		modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		ModConfigs.register();
	}
}
