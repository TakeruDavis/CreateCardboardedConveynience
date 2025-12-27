package net.takerudavis.cardboarded_conveynience.util;

import com.simibubi.create.content.kinetics.chainConveyor.ServerChainConveyorHandler;
import com.simibubi.create.foundation.render.PlayerSkyhookRenderer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.takerudavis.cardboarded_conveynience.CardboardedConveynience;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SkyhookHelper {

    private static Set<UUID> clientHangingPlayers;
    private static Object2IntMap<UUID> serverHangingPlayers;
    private static boolean clientInitialized = false;
    private static boolean serverInitialized = false;

    // Grace period to prevent flicker during detection gaps
    // Stores the game tick when the grace period EXPIRES (not remaining ticks)
    private static final Map<UUID, Long> graceExpiryTick = new HashMap<>();

    @SuppressWarnings("unchecked")
    private static void initClient() {
        if (clientInitialized) return;
        clientInitialized = true;
        try {
            Field f = PlayerSkyhookRenderer.class.getDeclaredField("hangingPlayers");
            f.setAccessible(true);
            clientHangingPlayers = (Set<UUID>) f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException | NoClassDefFoundError e) {
            // Expected on server side
        }
    }

    @SuppressWarnings("unchecked")
    private static void initServer() {
        if (serverInitialized) return;
        serverInitialized = true;
        try {
            Field f = ServerChainConveyorHandler.class.getDeclaredField("hangingPlayers");
            f.setAccessible(true);
            serverHangingPlayers = (Object2IntMap<UUID>) f.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            CardboardedConveynience.LOGGER.error("Failed to access server hanging players field", e);
        }
    }

    /**
     * Check if the player is currently hanging from a skyhook/chain conveyor.
     * Works on both client and server side.
     * Note: Use isPlayerHangingWithGrace() for rendering to avoid flicker.
     */
    public static boolean isPlayerHanging(Player player) {
        UUID uuid = player.getUUID();

        // Try server-side first (works on both dedicated server and integrated server)
        initServer();
        if (serverHangingPlayers != null && serverHangingPlayers.containsKey(uuid)) {
            return true;
        }

        // Fall back to client-side check
        initClient();
        if (clientHangingPlayers != null && clientHangingPlayers.contains(uuid)) {
            return true;
        }

        return false;
    }

    /**
     * Safe flying check - returns false if abilities is null (during player initialization).
     */
    public static boolean isPlayerFlying(Player player) {
        Abilities abilities = player.getAbilities();
        return abilities != null && abilities.flying;
    }

    /**
     * Check if the player is hanging, with a grace period to prevent flicker.
     * Use this for rendering decisions to smooth over momentary detection gaps
     * that can occur during server tick lag spikes.
     *
     * Uses expiry-time approach so grace period works even when player is off-screen.
     */
    public static boolean isPlayerHangingWithGrace(Player player) {
        // Guard against calls during player initialization when level isn't set
        if (player.level() == null) {
            return false;
        }

        UUID uuid = player.getUUID();
        long currentTick = player.level().getGameTime();
        boolean isFlying = isPlayerFlying(player);
        int gracePeriod = isFlying ? ModConstants.GRACE_PERIOD_FLYING : ModConstants.GRACE_PERIOD_NORMAL;

        if (isPlayerHanging(player)) {
            // Currently hanging - set expiry to current tick + grace period
            graceExpiryTick.put(uuid, currentTick + gracePeriod);
            return true;
        }

        // Not hanging - check if we're still within grace period
        Long expiryTick = graceExpiryTick.get(uuid);
        if (expiryTick != null) {
            if (currentTick < expiryTick) {
                // If now flying, shorten the expiry if needed
                if (isFlying) {
                    long flyingExpiry = currentTick + ModConstants.GRACE_PERIOD_FLYING;
                    if (flyingExpiry < expiryTick) {
                        graceExpiryTick.put(uuid, flyingExpiry);
                    }
                }
                return true; // Still in grace period
            }
            // Grace expired - clean up
            graceExpiryTick.remove(uuid);
        }

        return false;
    }

    /**
     * Clear grace period tracking for a player.
     * Call this when player explicitly dismounts or logs out.
     */
    public static void clearGracePeriod(Player player) {
        UUID uuid = player.getUUID();
        graceExpiryTick.remove(uuid);
    }
}
