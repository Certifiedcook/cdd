package dev.cd.diagnostics.module;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class FreecamModule {
    private static boolean active;
    private static double x;
    private static double y;
    private static double z;
    private static float yaw;
    private static float pitch;
    private static Vec3 velocity = Vec3.ZERO;
    private static long lastFrameNanos;

    private FreecamModule() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void toggle(Minecraft client) {
        if (active) disable(); else enable(client);
    }

    public static void enable(Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null) return;
        Camera camera = client.gameRenderer.mainCamera();
        if (camera == null || !camera.isInitialized()) return;

        Vec3 position = camera.position();
        x = position.x;
        y = position.y;
        z = position.z;
        yaw = camera.yRot();
        pitch = camera.xRot();
        velocity = Vec3.ZERO;
        lastFrameNanos = System.nanoTime();
        active = true;
    }

    public static void disable() {
        active = false;
        velocity = Vec3.ZERO;
        lastFrameNanos = 0L;
    }

    public static void updateFrame(Minecraft client) {
        if (!active) return;
        if (client.player == null || client.level == null) {
            disable();
            return;
        }

        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return;
        }
        double dt = Math.min((now - lastFrameNanos) / 1_000_000_000.0, 0.05);
        lastFrameNanos = now;

        if (client.gui.screen() != null || !client.isWindowActive()) {
            velocity = Vec3.ZERO;
            return;
        }

        Options options = client.options;
        double forwardAmount = axis(options.keyUp.isDown(), options.keyDown.isDown());
        double strafeAmount = axis(options.keyRight.isDown(), options.keyLeft.isDown());
        double verticalAmount = axis(options.keyJump.isDown(), options.keyShift.isDown());
        boolean boost = options.keySprint.isDown();

        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);

        Vec3 forward = new Vec3(
                -Math.sin(yawRadians) * Math.cos(pitchRadians),
                -Math.sin(pitchRadians),
                Math.cos(yawRadians) * Math.cos(pitchRadians)
        );
        Vec3 right = new Vec3(-Math.cos(yawRadians), 0.0, -Math.sin(yawRadians));
        Vec3 desired = forward.scale(forwardAmount).add(right.scale(strafeAmount)).add(0.0, verticalAmount, 0.0);
        if (desired.lengthSqr() > 1.0) desired = desired.normalize();

        double speed = FreecamSettings.speed() * (boost ? FreecamSettings.boostMultiplier() : 1.0);
        Vec3 targetVelocity = desired.scale(speed);

        if (FreecamSettings.smoothing()) {
            double response = 1.0 - Math.exp(-FreecamSettings.smoothingStrength() * dt);
            velocity = velocity.add(targetVelocity.subtract(velocity).scale(response));
        } else {
            velocity = targetVelocity;
        }

        Vec3 delta = velocity.scale(dt);
        x += delta.x;
        y += delta.y;
        z += delta.z;
    }

    public static void rotate(double yawInput, double pitchInput) {
        if (!active) return;
        double sensitivity = FreecamSettings.mouseSensitivity();
        yaw = Mth.wrapDegrees(yaw + (float) (yawInput * 0.15 * sensitivity));
        pitch = Mth.clamp(pitch + (float) (pitchInput * 0.15 * sensitivity), -90.0F, 90.0F);
    }

    public static Pose pose() {
        return new Pose(x, y, z, yaw, pitch);
    }

    private static double axis(boolean positive, boolean negative) {
        if (positive == negative) return 0.0;
        return positive ? 1.0 : -1.0;
    }

    public record Pose(double x, double y, double z, float yaw, float pitch) {
    }
}
