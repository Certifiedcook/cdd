package dev.cd.diagnostics.mixin;

import dev.cd.diagnostics.server.ServerTpsEstimator;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleSetTime(Lnet/minecraft/network/protocol/game/ClientboundSetTimePacket;)V", at = @At("HEAD"))
    private void cdd$observeServerTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        ServerTpsEstimator.observe(packet.gameTime());
    }
}
