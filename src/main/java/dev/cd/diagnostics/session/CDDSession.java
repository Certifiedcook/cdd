package dev.cd.diagnostics.session;

import dev.cd.diagnostics.DiagnosticsSettings;
import dev.cd.diagnostics.input.CDDKeyMappings;
import dev.cd.diagnostics.module.FakePlayerModule;
import dev.cd.diagnostics.module.FreecamModule;
import dev.cd.diagnostics.notification.CDDNotifications;
import net.minecraft.client.Minecraft;

public final class CDDSession {
    private static boolean panicActive;

    private CDDSession() {
    }

    public static boolean isPanicActive() {
        return panicActive;
    }

    public static boolean overlaysVisible() {
        if (panicActive) return false;
        return !DiagnosticsSettings.temporaryOverlayMode || CDDKeyMappings.TEMP_OVERLAY.isDown();
    }

    public static void togglePanic(Minecraft client) {
        panicActive = !panicActive;

        if (panicActive) {
            FreecamModule.disable();
            FakePlayerModule.remove();
            CDDNotifications.show("Panic enabled", "Overlays suspended; freecam stopped");
        } else {
            CDDNotifications.show("Panic cleared", "Saved overlay settings are active again");
        }
    }

    public static void clearPanic() {
        panicActive = false;
    }
}
