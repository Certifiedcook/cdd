package dev.cd.diagnostics.mixin;

import dev.cd.diagnostics.module.FreecamModule;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftInteractionMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void cdd$blockAttack(CallbackInfoReturnable<Boolean> cir) {
        if (FreecamModule.isActive()) cir.setReturnValue(false);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void cdd$blockContinuousAttack(boolean attacking, CallbackInfo ci) {
        if (FreecamModule.isActive()) ci.cancel();
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void cdd$blockUse(CallbackInfo ci) {
        if (FreecamModule.isActive()) ci.cancel();
    }
}
