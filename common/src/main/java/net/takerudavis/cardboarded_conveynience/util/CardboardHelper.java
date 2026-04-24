package net.takerudavis.cardboarded_conveynience.util;

import com.google.common.cache.Cache;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.equipment.armor.CardboardArmorHandlerClient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class CardboardHelper {

    private static Cache<UUID, Integer> BOXES_PLAYERS_ARE_HIDING_AS;

    // Cached armor items (lazily initialized)
    private static Map<EquipmentSlot, Item> cachedArmorItems;

    // 1.21.1: Use ResourceLocation.fromNamespaceAndPath instead of constructor
    private static final Map<EquipmentSlot, ResourceLocation> CARDBOARD_ARMOR_IDS = Map.of(
            EquipmentSlot.HEAD, ResourceLocation.fromNamespaceAndPath("create", "cardboard_helmet"),
            EquipmentSlot.CHEST, ResourceLocation.fromNamespaceAndPath("create", "cardboard_chestplate"),
            EquipmentSlot.LEGS, ResourceLocation.fromNamespaceAndPath("create", "cardboard_leggings"),
            EquipmentSlot.FEET, ResourceLocation.fromNamespaceAndPath("create", "cardboard_boots")
    );

    @SuppressWarnings("unchecked")
    private static void initLinked() {
        try {
            Field f = CardboardArmorHandlerClient.class.getDeclaredField("BOXES_PLAYERS_ARE_HIDING_AS");
            f.setAccessible(true);
            BOXES_PLAYERS_ARE_HIDING_AS = (Cache<UUID, Integer>) f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            CardboardedConveynience.LOGGER.error("Failed to access cardboard box cache field", e);
        }
    }

    private static void initArmorCache() {
        if (cachedArmorItems != null) return;
        cachedArmorItems = new java.util.EnumMap<>(EquipmentSlot.class);
        for (Map.Entry<EquipmentSlot, ResourceLocation> entry : CARDBOARD_ARMOR_IDS.entrySet()) {
            cachedArmorItems.put(entry.getKey(), BuiltInRegistries.ITEM.get(entry.getValue()));
        }
    }

    /**
     * Check that the player is wearing the full cardboard armor set.
     */
    public static boolean testForArmor(Player player) {
        initArmorCache();
        for (Map.Entry<EquipmentSlot, Item> entry : cachedArmorItems.entrySet()) {
            ItemStack stack = player.getItemBySlot(entry.getKey());
            Item expected = entry.getValue();

            if (expected == null || !expected.equals(stack.getItem())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Extended stealth check that also allows players hanging from chain conveyors.
     * Create's testForStealth() requires CROUCHING pose, which isn't consistent
     * when riding a chain conveyor. This bypasses the pose check for hanging players.
     */
    public static boolean testForStealthExtended(Player player) {
        // Must be wearing full cardboard armor
        if (!testForArmor(player)) {
            return false;
        }

        // Hanging players bypass pose/flying checks
        if (SkyhookHelper.isPlayerHangingWithGrace(player)) {
            return true;
        }

        // Not hanging - apply normal stealth rules
        if (SkyhookHelper.isPlayerFlying(player)) {
            return false;
        }

        return player.getPose() == net.minecraft.world.entity.Pose.CROUCHING;
    }

    /**
     * Get or create the current cardboard box index for the given player.
     */
    public static Integer getCurrentBoxIndex(Player player) throws ExecutionException {
        if (BOXES_PLAYERS_ARE_HIDING_AS == null) {
            initLinked();
        }
        return BOXES_PLAYERS_ARE_HIDING_AS.get(
                player.getUUID(),
                () -> player.level().random.nextInt(AllPartialModels.PACKAGES_TO_HIDE_AS.size())
        );
    }
}
