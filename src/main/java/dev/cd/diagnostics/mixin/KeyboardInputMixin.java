package dev.cd.diagnostics.mixin;

import dev.cd.diagnostics.module.FreecamModule;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
    @Inject(method = "tick()V", at = @At("RETURN"))
    private void cdd$freezePlayerInput(CallbackInfo ci) {
        if (!FreecamModule.isActive()) return;
        ClientInput input = (ClientInput) (Object) this;
        input.keyPresses = Input.EMPTY;
        ((ClientInputAccessor) input).cdd$setMoveVector(Vec2.ZERO);
    }
}
