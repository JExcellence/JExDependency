package me.devnatan.inventoryframework.runtime.thirdparty;

/**
 * Simple version helper used in tests to toggle behaviour that depends on the
 * Minecraft version supported by the Inventory Framework bridge.
 */
public final class McVersion {

    private static boolean supportsModern = true;

    private McVersion() {}

    public static boolean supports(int version) {
        return supportsModern;
    }

    public static void setSupportsModern(boolean value) {
        supportsModern = value;
    }

    public static void reset() {
        supportsModern = true;
    }
}
