package net.takerudavis.cardboarded_conveynience.util;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side cache for preemptively queried routes.
 * Stores route responses from the server for upcoming junctions.
 */
public class RouteCache {

    // Maps junction position -> connection to take (BlockPos.ZERO means no route)
    private static final Map<BlockPos, BlockPos> cachedRoutes = new HashMap<>();

    // Track which junction we've already queried for (to avoid duplicate queries)
    private static BlockPos pendingQueryJunction = null;

    /**
     * Cache a route response from the server.
     */
    public static void cacheRoute(BlockPos junctionPos, BlockPos routeConnection) {
        cachedRoutes.put(junctionPos, routeConnection);
        if (junctionPos.equals(pendingQueryJunction)) {
            pendingQueryJunction = null;
        }
    }

    /**
     * Get a cached route for a junction, or null if not cached.
     */
    @Nullable
    public static BlockPos getCachedRoute(BlockPos junctionPos) {
        return cachedRoutes.get(junctionPos);
    }

    /**
     * Check if we have a cached route for a junction.
     */
    public static boolean hasRoute(BlockPos junctionPos) {
        return cachedRoutes.containsKey(junctionPos);
    }

    /**
     * Remove a cached route (after using it or when it's no longer relevant).
     */
    public static void invalidate(BlockPos junctionPos) {
        cachedRoutes.remove(junctionPos);
    }

    /**
     * Clear all cached routes. Call when dismounting or destination changes.
     */
    public static void clearAll() {
        cachedRoutes.clear();
        pendingQueryJunction = null;
    }

    /**
     * Check if we've already sent a query for this junction.
     */
    public static boolean hasPendingQuery(BlockPos junctionPos) {
        return junctionPos.equals(pendingQueryJunction);
    }

    /**
     * Mark that we've sent a query for this junction.
     */
    public static void markQuerySent(BlockPos junctionPos) {
        pendingQueryJunction = junctionPos;
    }

    /**
     * Check if we should query for a junction (not already cached and no pending query).
     */
    public static boolean shouldQuery(BlockPos junctionPos) {
        return !hasRoute(junctionPos) && !hasPendingQuery(junctionPos);
    }
}
