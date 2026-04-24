package net.takerudavis.cardboarded_conveynience.neoforge;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;
import net.takerudavis.cardboarded_conveynience.advancement.ChuteTeleportTrigger;
import net.takerudavis.cardboarded_conveynience.advancement.DisguiseTrigger;
import net.takerudavis.cardboarded_conveynience.advancement.FrogportDeliveryTrigger;

/**
 * NeoForge-specific criteria registration using RegisterEvent
 * like Create does. This ensures proper timing with advancement loading.
 */
public class ModCriteriaNeoForge {

    public static final DisguiseTrigger DISGUISE_AS_PACKAGE = new DisguiseTrigger();
    public static final FrogportDeliveryTrigger FROGPORT_DELIVERY = new FrogportDeliveryTrigger();
    public static final ChuteTeleportTrigger CHUTE_TELEPORT = new ChuteTeleportTrigger();

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModCriteriaNeoForge::onRegister);
    }

    private static void onRegister(RegisterEvent event) {
        if (event.getRegistry() == BuiltInRegistries.TRIGGER_TYPES) {
            CardboardedConveynience.LOGGER.info("Registering criterion triggers...");

            Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                ResourceLocation.fromNamespaceAndPath(CardboardedConveynience.MOD_ID, "disguise_as_package"),
                DISGUISE_AS_PACKAGE);

            Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                ResourceLocation.fromNamespaceAndPath(CardboardedConveynience.MOD_ID, "frogport_delivery"),
                FROGPORT_DELIVERY);

            Registry.register(BuiltInRegistries.TRIGGER_TYPES,
                ResourceLocation.fromNamespaceAndPath(CardboardedConveynience.MOD_ID, "chute_teleport"),
                CHUTE_TELEPORT);

            CardboardedConveynience.LOGGER.info("Criterion triggers registered successfully");
        }
    }
}
