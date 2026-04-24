package net.takerudavis.cardboarded_conveynience.util;

/**
 * Central location for mod constants, making them easy to find and adjust.
 */
public final class ModConstants {

    private ModConstants() {} // Prevent instantiation

    // ===== Chain Conveyor Position Offsets =====
    // These adjust the player's position when riding the chain conveyor in cardboard armor

    /** Base Y offset for player position on chain conveyor */
    public static final double CHAIN_POSITION_OFFSET = 0.3D;

    /** Additional Y offset when player is flying (added to base) */
    public static final double CHAIN_POSITION_FLYING_OFFSET = -0.2D;

    // ===== Rendering Offsets =====
    // These adjust visual rendering of the cardboard box and wrench

    /** Y offset for wrench rendering when hanging from chain */
    public static final double WRENCH_RENDER_OFFSET = 0.7D;

    /** Y offset for package rendering relative to wrench */
    public static final double PACKAGE_RENDER_OFFSET = -0.9D;

    /** Base Y offset for cardboard box rendering */
    public static final float BOX_RENDER_OFFSET = 0.125F;

    // ===== Grace Period Settings =====
    // These control how long the "hanging" state persists after dismounting (prevents flicker)

    /** Grace period in ticks for normal players (0.6 seconds) */
    public static final int GRACE_PERIOD_NORMAL = 12;

    /** Grace period in ticks for flying players (0.25 seconds) */
    public static final int GRACE_PERIOD_FLYING = 5;
}
