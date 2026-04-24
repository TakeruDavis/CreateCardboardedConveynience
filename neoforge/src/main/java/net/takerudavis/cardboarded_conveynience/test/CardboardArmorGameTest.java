package net.takerudavis.cardboarded_conveynience.test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;
import net.takerudavis.cardboarded_conveynience.util.CardboardHelper;

/**
 * GameTests for CardboardHelper - the core armor detection logic.
 *
 * "Time to unwrap some bugs!" - Ryan
 */
@GameTestHolder(CardboardedConveynience.MOD_ID)
public class CardboardArmorGameTest {

    // Create's cardboard armor item IDs
    private static final ResourceLocation CARDBOARD_HELMET = ResourceLocation.fromNamespaceAndPath("create", "cardboard_helmet");
    private static final ResourceLocation CARDBOARD_CHESTPLATE = ResourceLocation.fromNamespaceAndPath("create", "cardboard_chestplate");
    private static final ResourceLocation CARDBOARD_LEGGINGS = ResourceLocation.fromNamespaceAndPath("create", "cardboard_leggings");
    private static final ResourceLocation CARDBOARD_BOOTS = ResourceLocation.fromNamespaceAndPath("create", "cardboard_boots");

    // ========== testForArmor TESTS ==========

    /**
     * Test that full cardboard armor returns true.
     */
    @GameTest(template = "empty_3x3")
    public void fullCardboardArmorReturnsTrue(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        equipFullCardboardArmor(player);

        boolean result = CardboardHelper.testForArmor(player);

        helper.assertTrue(result, "Full cardboard armor should return true");
        helper.succeed();
    }

    /**
     * Test that missing any single armor piece returns false.
     */
    @GameTest(template = "empty_3x3")
    public void missingAnyPieceReturnsFalse(GameTestHelper helper) {
        testSlotReplacementFailsArmor(helper, slot -> ItemStack.EMPTY, "Missing");
        helper.succeed();
    }

    /**
     * Test that wrong armor type in any slot returns false.
     */
    @GameTest(template = "empty_3x3")
    public void wrongArmorInAnySlotReturnsFalse(GameTestHelper helper) {
        testSlotReplacementFailsArmor(helper, this::getIronArmorForSlot, "Iron");
        helper.succeed();
    }

    // ========== testForStealthExtended TESTS ==========

    /**
     * Test that crouching player with full armor passes extended stealth.
     */
    @GameTest(template = "empty_3x3")
    public void crouchingWithArmorPassesStealth(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        equipFullCardboardArmor(player);
        player.setPose(Pose.CROUCHING);

        boolean result = CardboardHelper.testForStealthExtended(player);

        helper.assertTrue(result, "Crouching player with armor should pass stealth");
        helper.succeed();
    }

    /**
     * Test that standing player with full armor fails extended stealth.
     */
    @GameTest(template = "empty_3x3")
    public void standingWithArmorFailsStealth(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        equipFullCardboardArmor(player);
        player.setPose(Pose.STANDING);

        boolean result = CardboardHelper.testForStealthExtended(player);

        helper.assertTrue(!result, "Standing player with armor should fail stealth");
        helper.succeed();
    }

    /**
     * Test that crouching player without armor fails extended stealth.
     */
    @GameTest(template = "empty_3x3")
    public void crouchingWithoutArmorFailsStealth(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // No armor
        player.setPose(Pose.CROUCHING);

        boolean result = CardboardHelper.testForStealthExtended(player);

        helper.assertTrue(!result, "Crouching player without armor should fail stealth");
        helper.succeed();
    }

    // ========== HELPER METHODS ==========

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * Test that replacing any armor slot with the provided item fails armor check.
     */
    private void testSlotReplacementFailsArmor(
            GameTestHelper helper,
            java.util.function.Function<EquipmentSlot, ItemStack> replacementProvider,
            String testName) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            equipFullCardboardArmor(player);
            player.setItemSlot(slot, replacementProvider.apply(slot));

            boolean result = CardboardHelper.testForArmor(player);
            helper.assertTrue(!result, testName + " " + slot.getName() + " should return false");
        }
    }

    /**
     * Get iron armor piece for a given slot.
     */
    private ItemStack getIronArmorForSlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> new ItemStack(Items.IRON_HELMET);
            case CHEST -> new ItemStack(Items.IRON_CHESTPLATE);
            case LEGS -> new ItemStack(Items.IRON_LEGGINGS);
            case FEET -> new ItemStack(Items.IRON_BOOTS);
            default -> ItemStack.EMPTY;
        };
    }

    /**
     * Get a cardboard armor item from the registry.
     */
    private ItemStack getCardboardItem(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item);
    }

    /**
     * Equip a player with full cardboard armor set.
     */
    private void equipFullCardboardArmor(Player player) {
        player.setItemSlot(EquipmentSlot.HEAD, getCardboardItem(CARDBOARD_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, getCardboardItem(CARDBOARD_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, getCardboardItem(CARDBOARD_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, getCardboardItem(CARDBOARD_BOOTS));
    }
}
