package dev.cd.diagnostics;

import dev.cd.diagnostics.command.CDDChatCommands;
import dev.cd.diagnostics.config.CDDConfigManager;
import dev.cd.diagnostics.gui.DiagnosticsScreen;
import dev.cd.diagnostics.input.CDDKeyMappings;
import dev.cd.diagnostics.module.FakePlayerModule;
import dev.cd.diagnostics.module.FreecamModule;
import dev.cd.diagnostics.module.OreScanner;
import dev.cd.diagnostics.notification.CDDNotifications;
import dev.cd.diagnostics.render.DiagnosticsOverlayRenderer;
import dev.cd.diagnostics.session.CDDSession;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class CDDiagnosticsClient implements ClientModInitializer {
    public static final String MOD_ID = "cd_diagnostics";

    @Override
    public void onInitializeClient() {
        CDDConfigManager.load();
        CDDKeyMappings.bootstrap();
        DiagnosticsOverlayRenderer.initialize();
        CDDChatCommands.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            FakePlayerModule.tick(client);
            OreScanner.tick(client);

            if (client.level == null) {
                FreecamModule.disable();
                CDDSession.clearPanic();
            }

            while (CDDKeyMappings.PANIC.consumeClick()) {
                CDDSession.togglePanic(client);
            }

            while (CDDKeyMappings.GUI.consumeClick()) {
                if (client.gui.screen() == null) client.gui.setScreen(new DiagnosticsScreen(null));
            }

            while (CDDKeyMappings.FREECAM.consumeClick()) {
                if (client.gui.screen() != null) continue;
                if (CDDSession.isPanicActive()) {
                    CDDNotifications.show("Panic is active", "Clear Panic before enabling freecam");
                    continue;
                }

                FreecamModule.toggle(client);
                CDDNotifications.show("Freecam", FreecamModule.isActive() ? "Enabled" : "Disabled");
            }
        });
    }
}
