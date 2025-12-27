package net.takerudavis.cardboarded_conveynience.test;

import com.simibubi.create.content.logistics.box.PackageItem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.takerudavis.cardboarded_conveynience.util.CardboardHelper;
import net.takerudavis.cardboarded_conveynience.util.RouteCache;

/**
 * GameTests for Nametag Routing feature.
 * Tests RouteCache behavior, nametag detection, and pattern matching.
 *
 * "Time to cache some bugs... I mean routes!" - Ryan
 */
public class NametagRoutingGameTest implements FabricGameTest {

    // Create's cardboard armor item IDs
    private static final ResourceLocation CARDBOARD_HELMET = new ResourceLocation("create", "cardboard_helmet");
    private static final ResourceLocation CARDBOARD_CHESTPLATE = new ResourceLocation("create", "cardboard_chestplate");
    private static final ResourceLocation CARDBOARD_LEGGINGS = new ResourceLocation("create", "cardboard_leggings");
    private static final ResourceLocation CARDBOARD_BOOTS = new ResourceLocation("create", "cardboard_boots");

    // ========== ROUTECACHE TESTS ==========

    /**
     * Test basic cache and retrieve functionality.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void routeCacheCacheAndRetrieve(GameTestHelper helper) {
        // Clear any previous state
        RouteCache.clearAll();

        BlockPos junction = new BlockPos(10, 64, 20);
        BlockPos route = new BlockPos(11, 64, 20);

        // Initially should not have route
        helper.assertTrue(!RouteCache.hasRoute(junction), "Junction should not have route initially");
        helper.assertTrue(RouteCache.getCachedRoute(junction) == null, "getCachedRoute should return null initially");

        // Cache a route
        RouteCache.cacheRoute(junction, route);

        // Should now have route
        helper.assertTrue(RouteCache.hasRoute(junction), "Junction should have route after caching");
        helper.assertTrue(route.equals(RouteCache.getCachedRoute(junction)), "getCachedRoute should return cached route");

        RouteCache.clearAll();
        helper.succeed();
    }

    /**
     * Test cache invalidation.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void routeCacheInvalidate(GameTestHelper helper) {
        RouteCache.clearAll();

        BlockPos junction = new BlockPos(100, 64, 100);
        BlockPos route = new BlockPos(101, 64, 100);

        RouteCache.cacheRoute(junction, route);
        helper.assertTrue(RouteCache.hasRoute(junction), "Should have route after caching");

        RouteCache.invalidate(junction);
        helper.assertTrue(!RouteCache.hasRoute(junction), "Should not have route after invalidation");

        RouteCache.clearAll();
        helper.succeed();
    }

    /**
     * Test clearAll removes all cached routes.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void routeCacheClearAll(GameTestHelper helper) {
        RouteCache.clearAll();

        BlockPos junction1 = new BlockPos(1, 1, 1);
        BlockPos junction2 = new BlockPos(2, 2, 2);
        BlockPos junction3 = new BlockPos(3, 3, 3);

        RouteCache.cacheRoute(junction1, new BlockPos(10, 10, 10));
        RouteCache.cacheRoute(junction2, new BlockPos(20, 20, 20));
        RouteCache.cacheRoute(junction3, new BlockPos(30, 30, 30));

        helper.assertTrue(RouteCache.hasRoute(junction1), "Should have junction1");
        helper.assertTrue(RouteCache.hasRoute(junction2), "Should have junction2");
        helper.assertTrue(RouteCache.hasRoute(junction3), "Should have junction3");

        RouteCache.clearAll();

        helper.assertTrue(!RouteCache.hasRoute(junction1), "Should not have junction1 after clearAll");
        helper.assertTrue(!RouteCache.hasRoute(junction2), "Should not have junction2 after clearAll");
        helper.assertTrue(!RouteCache.hasRoute(junction3), "Should not have junction3 after clearAll");

        helper.succeed();
    }

    /**
     * Test pending query tracking.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void routeCachePendingQuery(GameTestHelper helper) {
        RouteCache.clearAll();

        BlockPos junction = new BlockPos(50, 50, 50);

        // Initially no pending query
        helper.assertTrue(!RouteCache.hasPendingQuery(junction), "Should not have pending query initially");
        helper.assertTrue(RouteCache.shouldQuery(junction), "Should query initially");

        // Mark query sent
        RouteCache.markQuerySent(junction);
        helper.assertTrue(RouteCache.hasPendingQuery(junction), "Should have pending query after marking");
        helper.assertTrue(!RouteCache.shouldQuery(junction), "Should not query while pending");

        // After caching route, pending should clear
        RouteCache.cacheRoute(junction, new BlockPos(51, 50, 50));
        helper.assertTrue(!RouteCache.hasPendingQuery(junction), "Pending should clear after cache response");
        helper.assertTrue(!RouteCache.shouldQuery(junction), "Should not query when route exists");

        RouteCache.clearAll();
        helper.succeed();
    }

    /**
     * Test shouldQuery logic with various states.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void routeCacheShouldQuery(GameTestHelper helper) {
        RouteCache.clearAll();

        BlockPos junction = new BlockPos(75, 75, 75);

        // Fresh junction - should query
        helper.assertTrue(RouteCache.shouldQuery(junction), "Fresh junction should query");

        // Pending query - should NOT query
        RouteCache.markQuerySent(junction);
        helper.assertTrue(!RouteCache.shouldQuery(junction), "Pending junction should not query");

        // Clear and cache route - should NOT query
        RouteCache.clearAll();
        RouteCache.cacheRoute(junction, new BlockPos(76, 75, 75));
        helper.assertTrue(!RouteCache.shouldQuery(junction), "Cached junction should not query");

        RouteCache.clearAll();
        helper.succeed();
    }

    /**
     * Test "no route" response handling (BlockPos.ZERO).
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void routeCacheNoRouteResponse(GameTestHelper helper) {
        RouteCache.clearAll();

        BlockPos junction = new BlockPos(200, 64, 200);

        // Server returns BlockPos.ZERO meaning "no valid route"
        RouteCache.cacheRoute(junction, BlockPos.ZERO);

        helper.assertTrue(RouteCache.hasRoute(junction), "Should have route entry for no-route response");
        helper.assertTrue(BlockPos.ZERO.equals(RouteCache.getCachedRoute(junction)),
            "getCachedRoute should return ZERO for no-route response");

        RouteCache.clearAll();
        helper.succeed();
    }

    // ========== NAMETAG DETECTION TESTS ==========

    /**
     * Test that named nametag in offhand is detected.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void namedNametagInOffhandDetected(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();

        // Put named nametag in offhand
        ItemStack nametag = new ItemStack(Items.NAME_TAG);
        nametag.setHoverName(Component.literal("Storage"));
        player.setItemSlot(EquipmentSlot.OFFHAND, nametag);

        ItemStack offhand = player.getOffhandItem();
        helper.assertTrue(offhand.is(Items.NAME_TAG), "Offhand should be nametag");
        helper.assertTrue(offhand.hasCustomHoverName(), "Nametag should have custom name");
        helper.assertTrue("Storage".equals(offhand.getHoverName().getString()), "Name should be 'Storage'");

        helper.succeed();
    }

    /**
     * Test that unnamed nametag is NOT valid for routing.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void unnamedNametagNotValidForRouting(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();

        // Put unnamed nametag in offhand
        ItemStack nametag = new ItemStack(Items.NAME_TAG);
        player.setItemSlot(EquipmentSlot.OFFHAND, nametag);

        ItemStack offhand = player.getOffhandItem();
        helper.assertTrue(offhand.is(Items.NAME_TAG), "Offhand should be nametag");
        helper.assertTrue(!offhand.hasCustomHoverName(), "Unnamed nametag should NOT have custom name");

        helper.succeed();
    }

    /**
     * Test that other items in offhand are not mistaken for nametags.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void nonNametagInOffhandIgnored(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();

        // Put a stick in offhand (not a nametag)
        ItemStack stick = new ItemStack(Items.STICK);
        stick.setHoverName(Component.literal("Storage"));
        player.setItemSlot(EquipmentSlot.OFFHAND, stick);

        ItemStack offhand = player.getOffhandItem();
        helper.assertTrue(!offhand.is(Items.NAME_TAG), "Stick should not be detected as nametag");

        helper.succeed();
    }

    /**
     * Test that empty offhand is handled correctly.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void emptyOffhandHandled(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();

        // Empty offhand
        player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

        ItemStack offhand = player.getOffhandItem();
        helper.assertTrue(!offhand.is(Items.NAME_TAG), "Empty offhand should not be nametag");

        helper.succeed();
    }

    // ========== PATTERN MATCHING TESTS (Uses Create's PackageItem.matchAddress) ==========

    /**
     * Test exact address matching.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void patternMatchExact(GameTestHelper helper) {
        helper.assertTrue(PackageItem.matchAddress("Storage", "Storage"), "Exact match should succeed");
        helper.assertTrue(!PackageItem.matchAddress("Storage", "Factory"), "Different addresses should not match");
        helper.assertTrue(!PackageItem.matchAddress("Storage", "storage"), "Case sensitivity test");

        helper.succeed();
    }

    /**
     * Test wildcard (*) pattern matching.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void patternMatchWildcardStar(GameTestHelper helper) {
        // Single * matches any sequence
        helper.assertTrue(PackageItem.matchAddress("Storage_A", "Storage_*"), "Wildcard should match suffix");
        helper.assertTrue(PackageItem.matchAddress("Storage_123", "Storage_*"), "Wildcard should match numbers");
        helper.assertTrue(PackageItem.matchAddress("Storage_", "Storage_*"), "Wildcard should match empty suffix");

        // * in middle
        helper.assertTrue(PackageItem.matchAddress("Area_Storage_Zone", "*_Storage_*"), "Wildcard in middle");

        // Leading *
        helper.assertTrue(PackageItem.matchAddress("Main_Storage", "*_Storage"), "Leading wildcard");

        helper.succeed();
    }

    /**
     * Test single character (?) pattern matching.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void patternMatchWildcardQuestion(GameTestHelper helper) {
        // ? matches exactly one character
        helper.assertTrue(PackageItem.matchAddress("Storage_A", "Storage_?"), "? should match single char");
        helper.assertTrue(PackageItem.matchAddress("Storage_1", "Storage_?"), "? should match single digit");
        helper.assertTrue(!PackageItem.matchAddress("Storage_AB", "Storage_?"), "? should not match two chars");
        helper.assertTrue(!PackageItem.matchAddress("Storage_", "Storage_?"), "? should not match empty");

        helper.succeed();
    }

    /**
     * Test complex pattern combinations.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void patternMatchComplex(GameTestHelper helper) {
        // Multiple wildcards
        helper.assertTrue(PackageItem.matchAddress("Zone_A_Storage_1", "Zone_?_Storage_*"), "Combined ? and *");
        helper.assertTrue(PackageItem.matchAddress("Zone_B_Storage_999", "Zone_?_Storage_*"), "Combined patterns");
        helper.assertTrue(!PackageItem.matchAddress("Zone_AB_Storage_1", "Zone_?_Storage_*"), "? strict single char");

        helper.succeed();
    }

    // ========== COMBINED ROUTING ELIGIBILITY TESTS ==========

    /**
     * Test full routing eligibility: cardboard armor + named nametag.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void fullRoutingEligibility(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();

        // Equip full cardboard armor
        equipFullCardboardArmor(player);

        // Put named nametag in offhand
        ItemStack nametag = new ItemStack(Items.NAME_TAG);
        nametag.setHoverName(Component.literal("Destination"));
        player.setItemSlot(EquipmentSlot.OFFHAND, nametag);

        // Check all conditions
        boolean hasArmor = CardboardHelper.testForArmor(player);
        ItemStack offhand = player.getOffhandItem();
        boolean hasNametag = offhand.is(Items.NAME_TAG);
        boolean hasName = offhand.hasCustomHoverName();
        String destination = hasName ? offhand.getHoverName().getString() : "";

        helper.assertTrue(hasArmor, "Should have cardboard armor");
        helper.assertTrue(hasNametag, "Should have nametag in offhand");
        helper.assertTrue(hasName, "Nametag should have name");
        helper.assertTrue("Destination".equals(destination), "Destination should be correct");

        helper.succeed();
    }

    /**
     * Test routing eligibility fails without armor.
     */
    @GameTest(template = EMPTY_STRUCTURE)
    public void routingRequiresArmor(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();

        // NO armor, but has named nametag
        ItemStack nametag = new ItemStack(Items.NAME_TAG);
        nametag.setHoverName(Component.literal("Destination"));
        player.setItemSlot(EquipmentSlot.OFFHAND, nametag);

        boolean hasArmor = CardboardHelper.testForArmor(player);
        helper.assertTrue(!hasArmor, "Should NOT pass armor check without armor");

        helper.succeed();
    }

    // ========== HELPER METHODS ==========

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
