package net.takerudavis.cardboarded_conveynience.advancement;

import net.minecraft.resources.ResourceLocation;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;

import java.util.function.Supplier;

/**
 * Holds references to custom advancement criteria for the mod.
 * Platform-specific code handles actual registration (e.g., DeferredRegister on NeoForge).
 */
public class ModCriteria {

    // Resource locations for the triggers
    public static final ResourceLocation DISGUISE_AS_PACKAGE_ID =
        ResourceLocation.fromNamespaceAndPath(CardboardedConveynience.MOD_ID, "disguise_as_package");
    public static final ResourceLocation FROGPORT_DELIVERY_ID =
        ResourceLocation.fromNamespaceAndPath(CardboardedConveynience.MOD_ID, "frogport_delivery");
    public static final ResourceLocation CHUTE_TELEPORT_ID =
        ResourceLocation.fromNamespaceAndPath(CardboardedConveynience.MOD_ID, "chute_teleport");

    // Suppliers set by platform-specific code during registration
    private static Supplier<DisguiseTrigger> disguiseSupplier;
    private static Supplier<FrogportDeliveryTrigger> frogportSupplier;
    private static Supplier<ChuteTeleportTrigger> chuteSupplier;

    /**
     * Called by platform-specific registration to provide trigger suppliers.
     */
    public static void setSuppliers(
            Supplier<DisguiseTrigger> disguise,
            Supplier<FrogportDeliveryTrigger> frogport,
            Supplier<ChuteTeleportTrigger> chute
    ) {
        disguiseSupplier = disguise;
        frogportSupplier = frogport;
        chuteSupplier = chute;
    }

    public static DisguiseTrigger getDisguiseTrigger() {
        return disguiseSupplier != null ? disguiseSupplier.get() : null;
    }

    public static FrogportDeliveryTrigger getFrogportDeliveryTrigger() {
        return frogportSupplier != null ? frogportSupplier.get() : null;
    }

    public static ChuteTeleportTrigger getChuteTeleportTrigger() {
        return chuteSupplier != null ? chuteSupplier.get() : null;
    }
}
