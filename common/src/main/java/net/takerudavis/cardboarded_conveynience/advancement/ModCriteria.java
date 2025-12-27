package net.takerudavis.cardboarded_conveynience.advancement;

import net.minecraft.advancements.CriteriaTriggers;

/**
 * Holds and registers all custom advancement criteria for the mod.
 */
public class ModCriteria {

    public static final DisguiseTrigger DISGUISE_AS_PACKAGE = new DisguiseTrigger();
    public static final FrogportDeliveryTrigger FROGPORT_DELIVERY = new FrogportDeliveryTrigger();
    public static final ChuteTeleportTrigger CHUTE_TELEPORT = new ChuteTeleportTrigger();

    public static void register() {
        CriteriaTriggers.register(DISGUISE_AS_PACKAGE);
        CriteriaTriggers.register(FROGPORT_DELIVERY);
        CriteriaTriggers.register(CHUTE_TELEPORT);
    }
}
