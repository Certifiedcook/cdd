package dev.cd.diagnostics.mixin;

import dev.cd.diagnostics.module.FreecamModule;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(
            method = "update(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;calculateFov(F)F")
    )
    private void cdd$applyFreecam(DeltaTracker tracker, CallbackInfo ci) {
        if (!FreecamModule.isActive()) return;
        Minecraft client = Minecraft.getInstance();
        FreecamModule.updateFrame(client);
        FreecamModule.Pose pose = FreecamModule.pose();
        setPosition(pose.x(), pose.y(), pose.z());
        setRotation(pose.yaw(), pose.pitch());
        detached = true;
    }
}
