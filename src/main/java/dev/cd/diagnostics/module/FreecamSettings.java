package dev.cd.diagnostics.module;

public final class FreecamSettings {
    private static double speed = 12.0;
    private static double boostMultiplier = 3.0;
    private static boolean smoothing = true;
    private static double smoothingStrength = 10.0;
    private static double mouseSensitivity = 1.0;

    private FreecamSettings() {
    }

    public static double speed() { return speed; }
    public static double boostMultiplier() { return boostMultiplier; }
    public static boolean smoothing() { return smoothing; }
    public static double smoothingStrength() { return smoothingStrength; }
    public static double mouseSensitivity() { return mouseSensitivity; }

    public static void setSpeed(double value) { speed = clamp(value, 1.0, 80.0); }
    public static void setBoostMultiplier(double value) { boostMultiplier = clamp(value, 1.0, 10.0); }
    public static void toggleSmoothing() { smoothing = !smoothing; }
    public static void setSmoothingStrength(double value) { smoothingStrength = clamp(value, 1.0, 30.0); }
    public static void setMouseSensitivity(double value) { mouseSensitivity = clamp(value, 0.1, 4.0); }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
