package dev.cd.diagnostics;

public final class DiagnosticsSettings {
    public static boolean playerEsp = true;
    public static boolean playerTracers = false;

    public static boolean storageEsp = true;
    public static boolean storageDepthOverride = true;
    public static boolean showChests = true;
    public static boolean showBarrels = true;
    public static boolean showShulkers = true;
    public static boolean showEnderChests = true;

    public static boolean oreEsp = true;
    public static boolean oreDepthOverride = true;
    public static boolean showDiamond = true;
    public static boolean showAncientDebris = true;
    public static boolean showIron = true;
    public static boolean showGold = true;
    public static boolean showLapis = true;
    public static boolean showRedstone = true;

    public static boolean notifications = true;
    public static boolean temporaryOverlayMode = false;

    private DiagnosticsSettings() {
    }

    public static boolean anyOreEnabled() {
        return showDiamond
                || showAncientDebris
                || showIron
                || showGold
                || showLapis
                || showRedstone;
    }
}
