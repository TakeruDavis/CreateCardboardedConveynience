package net.takerudavis.cardboarded_conveynience.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of projectile types that can pass through belt tunnels.
 * Maps projectile class to the ItemStack used for brass tunnel filter matching.
 */
public class TunnelPassthroughRegistry {

    /**
     * Static mappings of projectile class to filter ItemStack.
     * Order matters for subclass checking (ThrownTrident before AbstractArrow).
     * Uses LinkedHashMap to preserve insertion order.
     */
    public static final Map<Class<? extends Entity>, ItemStack> STATIC_PROJECTILES;

    static {
        STATIC_PROJECTILES = new LinkedHashMap<>();
        // Order matters! More specific types first
        STATIC_PROJECTILES.put(ThrownTrident.class, new ItemStack(Items.TRIDENT));
        STATIC_PROJECTILES.put(AbstractArrow.class, new ItemStack(Items.ARROW));
        STATIC_PROJECTILES.put(FireworkRocketEntity.class, new ItemStack(Items.FIREWORK_ROCKET));
        STATIC_PROJECTILES.put(ThrownEnderpearl.class, new ItemStack(Items.ENDER_PEARL));
        STATIC_PROJECTILES.put(ThrownPotion.class, new ItemStack(Items.SPLASH_POTION));
        STATIC_PROJECTILES.put(Snowball.class, new ItemStack(Items.SNOWBALL));
        STATIC_PROJECTILES.put(FishingHook.class, new ItemStack(Items.FISHING_ROD));
    }

    /**
     * Get the filter ItemStack for a given entity, checking static mappings.
     *
     * @param entity The entity to check
     * @return The filter ItemStack, or null if not a registered projectile
     */
    public static ItemStack getFilterItem(Entity entity) {
        // Check in order (LinkedHashMap preserves insertion order)
        for (Map.Entry<Class<? extends Entity>, ItemStack> entry : STATIC_PROJECTILES.entrySet()) {
            if (entry.getKey().isInstance(entity)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Check if an entity is a registered static projectile type.
     */
    public static boolean isStaticProjectile(Entity entity) {
        return getFilterItem(entity) != null;
    }
}
