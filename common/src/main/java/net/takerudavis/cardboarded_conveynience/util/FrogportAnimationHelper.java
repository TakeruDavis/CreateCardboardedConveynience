package net.takerudavis.cardboarded_conveynience.util;

/**
 * Helper class to control Frogport animation bypass.
 *
 * This is a regular class (not a Mixin) so it can be safely referenced
 * from other code. The FrogportAnimationMixin reads from this helper.
 */
public class FrogportAnimationHelper {

    /**
     * Thread-local flag to bypass the isPackage check for the next startAnimation call.
     * Thread-local ensures safety if multiple threads somehow call this.
     */
    private static final ThreadLocal<Boolean> bypassPackageCheck =
        ThreadLocal.withInitial(() -> false);

    /**
     * Set the bypass flag before calling startAnimation.
     * The flag is automatically reset after one use by the Mixin.
     */
    public static void setBypassPackageCheck(boolean bypass) {
        bypassPackageCheck.set(bypass);
    }

    /**
     * Check and consume the bypass flag.
     * Called by FrogportAnimationMixin.
     */
    public static boolean consumeBypass() {
        boolean value = bypassPackageCheck.get();
        if (value) {
            bypassPackageCheck.set(false);
        }
        return value;
    }
}
