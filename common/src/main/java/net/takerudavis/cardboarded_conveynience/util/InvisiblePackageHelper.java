package net.takerudavis.cardboarded_conveynience.util;

import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;

/**
 * Helper for the invisible package used in Frogport animations.
 *
 * This registers an invisible model in Create's PACKAGES and PACKAGE_RIGGING maps,
 * but NOT in PACKAGES_TO_HIDE_AS (so players can't disguise as it).
 *
 * Uses BARRIER item as the carrier since it's already invisible and won't
 * interfere with normal gameplay.
 */
public class InvisiblePackageHelper {

    private static final ResourceLocation INVISIBLE_PACKAGE_KEY =
        new ResourceLocation("minecraft", "barrier");

    private static boolean registered = false;

    /**
     * Register the invisible package models. Call this on client init.
     * Safe to call multiple times - will only register once.
     */
    public static void register() {
        if (registered) return;
        registered = true;

        // Create partial models pointing to our invisible model files
        PartialModel invisiblePackage = PartialModel.of(
            new ResourceLocation(CardboardedConveynience.MOD_ID, "item/invisible_package")
        );
        PartialModel invisibleRigging = PartialModel.of(
            new ResourceLocation(CardboardedConveynience.MOD_ID, "item/invisible_rigging")
        );

        // Register in Create's maps (but NOT in PACKAGES_TO_HIDE_AS)
        AllPartialModels.PACKAGES.put(INVISIBLE_PACKAGE_KEY, invisiblePackage);
        AllPartialModels.PACKAGE_RIGGING.put(INVISIBLE_PACKAGE_KEY, invisibleRigging);
    }

    /**
     * Get an invisible package ItemStack for use in Frogport animations.
     * This will trigger the tongue animation without rendering a visible box.
     */
    public static ItemStack getInvisiblePackage() {
        return new ItemStack(Items.BARRIER);
    }
}
