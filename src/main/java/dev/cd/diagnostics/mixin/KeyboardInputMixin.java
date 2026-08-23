package dev.cd.diagnostics.mixin;

import dev.cd.diagnostics.module.FreecamModule;
import dev.cd.diagnostics.module.WalkingModule;
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
    private void cdd$overridePlayerInput(CallbackInfo ci) {
        ClientInput input = (ClientInput) (Object) this;

        if (FreecamModule.isActive()) {
            input.keyPresses = Input.EMPTY;
            ((ClientInputAccessor) input).cdd$setMoveVector(Vec2.ZERO);
            return;
        }

        if (!WalkingModule.isActive()) return;

        boolean forward = WalkingModule.shouldMoveForward();
        boolean jump = WalkingModule.shouldJump();
        boolean sneak = WalkingModule.shouldSneak();

        input.keyPresses = new Input(
                forward,
                false,
                false,
                false,
                jump,
                sneak,
                false
        );
        ((ClientInputAccessor) input).cdd$setMoveVector(forward ? new Vec2(0.0F, 1.0F) : Vec2.ZERO);
    }
}
