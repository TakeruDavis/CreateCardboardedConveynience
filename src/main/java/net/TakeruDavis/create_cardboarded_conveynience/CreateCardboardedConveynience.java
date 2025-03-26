package net.TakeruDavis.create_cardboarded_conveynience;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import mod.TakeruDavis.create_cardboarded_conveynience.BuildConfig;
import net.TakeruDavis.create_cardboarded_conveynience.register.config.ModConfigs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BuildConfig.MODID)
public class CreateCardboardedConveynience {
	// Directly reference a log4j logger.
	private static final Logger LOGGER = LogManager.getLogger(BuildConfig.MODID);
	public static IEventBus modEventBus;

	public static final NonNullSupplier<CreateRegistrate> registrate = NonNullSupplier.lazy(() -> CreateRegistrate.create(BuildConfig.MODID));

	public CreateCardboardedConveynience() {
		modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		CreateRegistrate r = registrate.get();
		ModConfigs.register();
	}
}
