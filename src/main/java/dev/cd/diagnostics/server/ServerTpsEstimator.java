package dev.cd.diagnostics.server;

public final class ServerTpsEstimator {
    private static long lastGameTime = Long.MIN_VALUE;
    private static long lastNanos;
    private static double estimatedTps = Double.NaN;
    private static int samples;

    private ServerTpsEstimator() {
    }

    public static void observe(long gameTime) {
        long now = System.nanoTime();

        if (lastGameTime != Long.MIN_VALUE && gameTime > lastGameTime && lastNanos != 0L) {
            long tickDelta = gameTime - lastGameTime;
            double seconds = (now - lastNanos) / 1_000_000_000.0;

            if (seconds >= 0.05 && seconds <= 30.0 && tickDelta > 0 && tickDelta <= 600L) {
                double raw = tickDelta / seconds;
                if (raw > 0.0 && raw <= 30.0) {
                    raw = Math.min(20.0, raw);
                    estimatedTps = Double.isNaN(estimatedTps)
                            ? raw
                            : estimatedTps * 0.72 + raw * 0.28;
                    samples++;
                }
            }
        }

        lastGameTime = gameTime;
        lastNanos = now;
    }

    public static boolean hasEstimate() {
        return !Double.isNaN(estimatedTps);
    }

    public static double estimatedTps() {
        return hasEstimate() ? estimatedTps : 0.0;
    }

    public static int samples() {
        return samples;
    }

    public static void reset() {
        lastGameTime = Long.MIN_VALUE;
        lastNanos = 0L;
        estimatedTps = Double.NaN;
        samples = 0;
    }
}
