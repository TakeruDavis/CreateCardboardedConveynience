package net.takerudavis.cardboarded_conveynience.mixin;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.takerudavis.cardboarded_conveynience.util.FrogportAnimationHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin to allow Frogport animation to play for players in cardboard armor.
 *
 * Normally, startAnimation() checks if the item is a package and returns early if not.
 * This mixin allows bypassing that check for player "catch" animations.
 *
 * The bypass flag is controlled via FrogportAnimationHelper (a regular class).
 */
@Mixin(FrogportBlockEntity.class)
public class FrogportAnimationMixin {

    /**
     * Redirect the PackageItem.isPackage() call in startAnimation to allow bypass.
     */
    @Redirect(
        method = "startAnimation",
        at = @At(
            value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/box/PackageItem;isPackage(Lnet/minecraft/world/item/ItemStack;)Z"
        ),
        remap = false
    )
    private boolean cardboarded_conveynience$redirectIsPackageCheck(ItemStack stack) {
        // Check if bypass is requested (consumes the flag)
        if (FrogportAnimationHelper.consumeBypass()) {
            // Pretend it's a package to allow animation
            return true;
        }
        // Normal behavior
        return PackageItem.isPackage(stack);
    }
}
