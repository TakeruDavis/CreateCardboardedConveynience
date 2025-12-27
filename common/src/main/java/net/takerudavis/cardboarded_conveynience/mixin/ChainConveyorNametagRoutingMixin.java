package net.takerudavis.cardboarded_conveynience.mixin;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.takerudavis.cardboarded_conveynience.network.CardboardedNetworking;
import net.takerudavis.cardboarded_conveynience.util.CardboardHelper;
import net.takerudavis.cardboarded_conveynience.util.DismountHelper;
import net.takerudavis.cardboarded_conveynience.util.RouteCache;
import net.takerudavis.cardboarded_conveynience.util.SkyhookHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to enable nametag-based routing for players on chain conveyors.
 *
 * When a player in full cardboard armor has a named nametag in their offhand,
 * they are automatically routed to the matching Frogport instead of using
 * sight-based steering.
 *
 * "Players become ACTUAL routable packages!" - Jordan
 */
@Mixin(ChainConveyorRidingHandler.class)
public class ChainConveyorNametagRoutingMixin {

    @Shadow
    public static BlockPos ridingChainConveyor;

    @Shadow
    public static float chainPosition;

    @Shadow
    public static BlockPos ridingConnection;

    // Flag to trigger dismount at start of next tick (avoids mid-frame null pointer crash)
    @Unique
    private static boolean cardboarded_conveynience$shouldDismount = false;

    // Store the matched Frogport position for animation trigger
    @Unique
    private static BlockPos cardboarded_conveynience$arrivalFrogportPos = null;

    // Track the last connection we were traveling on (to detect when we START a new connection)
    @Unique
    private static BlockPos cardboarded_conveynience$lastRidingConnection = null;

    // Track the player's destination address (to clear cache when it changes)
    @Unique
    private static String cardboarded_conveynience$lastDestination = null;

    // Cached Frogport data for the current connection (to avoid scanning travelPorts every tick)
    @Unique
    private static BlockPos cardboarded_conveynience$cachedFrogportPos = null;
    @Unique
    private static Vec3 cardboarded_conveynience$cachedGrabLocation = null;

    // How close the player needs to be to the Frogport's grab point (horizontally) - squared for efficiency
    @Unique
    private static final double ARRIVAL_HORIZONTAL_THRESHOLD_SQ = 1.0;

    // Vertical range to check (player hangs below chain, target is at chain level)
    @Unique
    private static final double ARRIVAL_VERTICAL_MIN = 0.0;  // Player can be at or above target
    @Unique
    private static final double ARRIVAL_VERTICAL_MAX = 3.0;  // Player hangs up to ~3 blocks below

    /**
     * Inject at HEAD of clientTick to handle deferred dismount.
     * This avoids the mid-frame null pointer crash by handling dismount
     * at the start of a fresh tick rather than mid-frame.
     */
    @Inject(
        method = "clientTick",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void cardboarded_conveynience$handleDeferredDismount(CallbackInfo ci) {
        if (cardboarded_conveynience$shouldDismount) {
            cardboarded_conveynience$shouldDismount = false;

            Minecraft mc = Minecraft.getInstance();

            // Send Frogport arrival packet to server for animation sync
            // Server will validate and call startAnimation, which syncs to all clients
            if (cardboarded_conveynience$arrivalFrogportPos != null) {
                CardboardedNetworking.sendFrogportArrival(cardboarded_conveynience$arrivalFrogportPos);
                cardboarded_conveynience$arrivalFrogportPos = null;
            }

            // Send dismount packet to server BEFORE nulling variables
            // This triggers ServerChainConveyorHandlerMixin.handleStopRidingPacket which fixes the pose
            DismountHelper.sendStopRidingPacket(ridingChainConveyor);

            // Now properly dismount the player (client-side)
            ridingChainConveyor = null;
            ridingConnection = null;

            // Also clear grace period on client side
            if (mc.player != null) {
                SkyhookHelper.clearGracePeriod(mc.player);
            }

            // Clear route cache and tracking variables
            RouteCache.clearAll();
            cardboarded_conveynience$lastRidingConnection = null;
            cardboarded_conveynience$lastDestination = null;
            cardboarded_conveynience$cachedFrogportPos = null;
            cardboarded_conveynience$cachedGrabLocation = null;

            // Cancel the entire clientTick to prevent any null pointer issues
            ci.cancel();
        }
    }

    /**
     * Inject at HEAD to handle both:
     * 1. Travelling along a connection (ridingConnection != null) - check for Frogport arrival
     * 2. At a junction (ridingConnection == null) - handle routing decisions
     */
    @Inject(
        method = "updateTargetPosition",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void cardboarded_conveynience$handleNametagRouting(
            Minecraft mc,
            ChainConveyorBlockEntity clbe,
            CallbackInfo ci
    ) {
        // Check if player qualifies for nametag routing
        if (mc.player == null) {
            return;
        }

        // Must be wearing full cardboard armor
        if (!CardboardHelper.testForArmor(mc.player)) {
            return;
        }

        // Must have nametag in offhand
        ItemStack offhand = mc.player.getOffhandItem();
        if (!offhand.is(Items.NAME_TAG)) {
            return;
        }

        // Nametag must have a custom name
        if (!offhand.hasCustomHoverName()) {
            return;
        }

        String destinationAddress = offhand.getHoverName().getString();
        if (destinationAddress.isEmpty()) {
            return;
        }

        // Clear cache if destination changed
        if (!destinationAddress.equals(cardboarded_conveynience$lastDestination)) {
            RouteCache.clearAll();
            cardboarded_conveynience$lastDestination = destinationAddress;
        }

        // CASE 1: Travelling along a connection - check for Frogport arrival and preemptive query
        if (ridingConnection != null) {
            // Detect if we just started traveling a NEW connection
            if (!ridingConnection.equals(cardboarded_conveynience$lastRidingConnection)) {
                cardboarded_conveynience$lastRidingConnection = ridingConnection;

                // Clear cached Frogport from previous connection
                cardboarded_conveynience$cachedFrogportPos = null;
                cardboarded_conveynience$cachedGrabLocation = null;

                // Cache matching Frogport on this connection (if any)
                cardboarded_conveynience$cacheConnectionFrogport(mc, clbe, destinationAddress);

                // We just started traveling to junction at ridingConnection - query it preemptively
                if (RouteCache.shouldQuery(ridingConnection)) {
                    RouteCache.markQuerySent(ridingConnection);
                    CardboardedNetworking.sendRouteQuery(ridingConnection, destinationAddress);
                }
            }

            cardboarded_conveynience$handleTravellingArrival(mc, clbe, destinationAddress, ci);
            return;
        }

        // Reset connection tracking when we arrive at a junction
        cardboarded_conveynience$lastRidingConnection = null;

        // CASE 2: At a junction - handle routing decisions using cached routes
        cardboarded_conveynience$handleJunctionRouting(mc, clbe, destinationAddress, ci);
    }

    /**
     * Cache the matching Frogport for the current connection.
     * Called once when starting a new connection to avoid per-tick scanning.
     */
    @Unique
    private static void cardboarded_conveynience$cacheConnectionFrogport(
            Minecraft mc,
            ChainConveyorBlockEntity clbe,
            String destinationAddress
    ) {
        for (var portEntry : clbe.travelPorts.entrySet()) {
            var port = portEntry.getValue();

            // Only check Frogports on the connection we're currently traveling
            if (!port.connection().equals(ridingConnection)) {
                continue;
            }

            // Check if this Frogport's filter matches our destination
            String filter = port.filter();
            if (!PackageItem.matchAddress(destinationAddress, filter)) {
                continue;
            }

            // Get the Frogport block entity to check grab location
            BlockPos frogportRelPos = portEntry.getKey();
            BlockPos frogportAbsPos = clbe.getBlockPos().offset(frogportRelPos);
            BlockEntity be = mc.level.getBlockEntity(frogportAbsPos);

            if (!(be instanceof FrogportBlockEntity frogport)) {
                continue;
            }

            if (frogport.target == null) {
                continue;
            }

            Vec3 grabLocation = frogport.target.getExactTargetLocation(frogport, mc.level, frogportAbsPos);
            if (grabLocation.equals(Vec3.ZERO)) {
                continue;
            }

            // Cache the Frogport data
            cardboarded_conveynience$cachedFrogportPos = frogportAbsPos;
            cardboarded_conveynience$cachedGrabLocation = grabLocation;
            return; // Only cache first matching Frogport
        }
    }

    /**
     * Handle Frogport arrival detection while travelling along a connection.
     * Uses cached Frogport data for efficient per-tick distance checks.
     */
    @Unique
    private static void cardboarded_conveynience$handleTravellingArrival(
            Minecraft mc,
            ChainConveyorBlockEntity clbe,
            String destinationAddress,
            CallbackInfo ci
    ) {
        // No cached Frogport on this connection - nothing to check
        if (cardboarded_conveynience$cachedFrogportPos == null ||
            cardboarded_conveynience$cachedGrabLocation == null) {
            return;
        }

        Vec3 playerPos = mc.player.position();
        Vec3 grabLocation = cardboarded_conveynience$cachedGrabLocation;

        // Check horizontal distance squared (X-Z plane) - avoids sqrt
        double dx = playerPos.x - grabLocation.x;
        double dz = playerPos.z - grabLocation.z;
        double horizontalDistSq = dx * dx + dz * dz;

        // Check vertical distance (player hangs below the chain)
        double verticalDiff = grabLocation.y - playerPos.y;

        // Player should be horizontally close and vertically below the grab point
        if (horizontalDistSq <= ARRIVAL_HORIZONTAL_THRESHOLD_SQ &&
            verticalDiff >= ARRIVAL_VERTICAL_MIN &&
            verticalDiff <= ARRIVAL_VERTICAL_MAX) {

            // Store the Frogport position for animation trigger
            cardboarded_conveynience$arrivalFrogportPos = cardboarded_conveynience$cachedFrogportPos;

            // Set flag for deferred dismount (handled at HEAD of next clientTick)
            cardboarded_conveynience$shouldDismount = true;

            ci.cancel();
        }
    }

    /**
     * Handle routing decisions at junctions (when ridingConnection is null).
     * Uses cached routes from server queries instead of local routing table.
     */
    @Unique
    private static void cardboarded_conveynience$handleJunctionRouting(
            Minecraft mc,
            ChainConveyorBlockEntity clbe,
            String destinationAddress,
            CallbackInfo ci
    ) {
        BlockPos junctionPos = clbe.getBlockPos();

        // Check if we have a cached route from server
        BlockPos targetConnection = RouteCache.getCachedRoute(junctionPos);

        // If no cached route, fall back to sight-steering
        if (targetConnection == null) {
            // Maybe we need to query? Send one if we haven't already
            if (RouteCache.shouldQuery(junctionPos)) {
                RouteCache.markQuerySent(junctionPos);
                CardboardedNetworking.sendRouteQuery(junctionPos, destinationAddress);
            }
            // Fall back to original sight-steering
            return;
        }

        // Server returned "no route" (BlockPos.ZERO means no valid route exists)
        if (targetConnection.equals(BlockPos.ZERO)) {
            // Clear this cache entry so we don't keep hitting it
            RouteCache.invalidate(junctionPos);
            return;
        }

        // We have a valid cached route - proceed with navigation

        // Calculate movement for this tick
        float serverSpeed = ServerSpeedProvider.get();
        float speed = clbe.getSpeed() / 360f;
        float radius = 1.5f;
        float degreesPerTick = (speed / (float)(Math.PI * radius)) * 360f;
        float prevChainPosition = chainPosition;

        // Update chain position
        chainPosition += serverSpeed * degreesPerTick;
        chainPosition = clbe.wrapAngle(chainPosition);

        // Verify this connection exists on this conveyor
        if (!clbe.connections.contains(targetConnection)) {
            chainPosition = prevChainPosition;
            return;
        }

        // Check if we've crossed the threshold for our target connection
        float offBranchAngle = clbe.connectionStats.get(targetConnection).tangentAngle();
        if (!clbe.loopThresholdCrossed(chainPosition, prevChainPosition, offBranchAngle)) {
            ci.cancel(); // We handled the position update, skip original
            return;
        }

        // Take the branch!
        chainPosition = 0;
        ridingConnection = targetConnection;

        // Invalidate the cache entry we just used (next visit should get fresh route)
        RouteCache.invalidate(junctionPos);

        // The preemptive query for targetConnection will happen in the next tick
        // when handleNametagRouting detects we started a new connection

        ci.cancel();
    }
}
