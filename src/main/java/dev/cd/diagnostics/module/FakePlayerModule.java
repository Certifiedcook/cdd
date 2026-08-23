package dev.cd.diagnostics.module;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class FakePlayerModule {
    private static final UUID UUID_VALUE = UUID.nameUUIDFromBytes("cd_diagnostics:fake_player".getBytes(StandardCharsets.UTF_8));
    private static final String NAME = "CD_TestPlayer";
    private static int nextEntityId = -10_000;
    private static RemotePlayer fakePlayer;
    private static ClientLevel fakePlayerLevel;

    private FakePlayerModule() {
    }

    public static boolean isSpawned() {
        return fakePlayer != null && !fakePlayer.isRemoved();
    }

    public static boolean spawn(Minecraft client) {
        if (client.player == null || client.level == null) return false;
        if (!client.hasSingleplayerServer()) {
            client.player.sendSystemMessage(Component.literal("[CD Diagnostics] Fake player is singleplayer-only."));
            return false;
        }

        remove();
        ClientLevel level = client.level;
        RemotePlayer dummy = new RemotePlayer(level, new GameProfile(UUID_VALUE, NAME));

        while (level.getEntity(nextEntityId) != null) nextEntityId--;
        dummy.setId(nextEntityId--);

        float yaw = client.player.getYRot();
        double radians = Math.toRadians(yaw);
        double x = client.player.getX() - Math.sin(radians) * 3.0;
        double y = client.player.getY();
        double z = client.player.getZ() + Math.cos(radians) * 3.0;
        dummy.setPos(x, y, z);
        dummy.setYRot(yaw + 180.0F);
        dummy.setXRot(0.0F);
        dummy.setCustomName(Component.literal("CD Test Player"));
        dummy.setCustomNameVisible(true);

        level.addEntity(dummy);
        fakePlayer = dummy;
        fakePlayerLevel = level;
        return true;
    }

    public static void remove() {
        if (fakePlayer != null && fakePlayerLevel != null && !fakePlayer.isRemoved()) {
            fakePlayerLevel.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
        }
        fakePlayer = null;
        fakePlayerLevel = null;
    }

    public static void toggle(Minecraft client) {
        if (isSpawned()) remove(); else spawn(client);
    }

    public static void tick(Minecraft client) {
        if (fakePlayer == null) return;
        if (client.level == null || client.level != fakePlayerLevel || !client.hasSingleplayerServer()) {
            remove();
        }
    }
}
