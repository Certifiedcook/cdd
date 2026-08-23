package dev.cd.diagnostics.mixin;

import dev.cd.diagnostics.module.FreecamModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "turnPlayer(D)V", at = @At("HEAD"), cancellable = true)
    private void cdd$redirectMouse(double frameDelta, CallbackInfo ci) {
        if (!FreecamModule.isActive()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() != null) return;

        double sensitivity = client.options.sensitivity().get() * 0.6000000238418579D + 0.20000000298023224D;
        double scale = sensitivity * sensitivity * sensitivity * 8.0D;
        double yawInput = accumulatedDX * scale;
        double pitchInput = accumulatedDY * scale;
        if (client.options.invertMouseX().get()) yawInput = -yawInput;
        if (client.options.invertMouseY().get()) pitchInput = -pitchInput;

        FreecamModule.rotate(yawInput, pitchInput);
        accumulatedDX = 0.0D;
        accumulatedDY = 0.0D;
        ci.cancel();
    }
}
