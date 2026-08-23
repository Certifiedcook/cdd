package dev.cd.diagnostics.command;

import dev.cd.diagnostics.DiagnosticsSettings;
import dev.cd.diagnostics.config.CDDConfigManager;
import dev.cd.diagnostics.gui.DiagnosticsScreen;
import dev.cd.diagnostics.input.CDDKeyMappings;
import dev.cd.diagnostics.module.FakePlayerModule;
import dev.cd.diagnostics.module.FreecamModule;
import dev.cd.diagnostics.module.FreecamSettings;
import dev.cd.diagnostics.notification.CDDNotifications;
import dev.cd.diagnostics.session.CDDSession;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;

import java.util.Locale;

public final class CDDChatCommands {
    private CDDChatCommands() {
    }

    public static void register() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            String trimmed = message.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (!lower.equals(".cdd") && !lower.startsWith(".cdd ")) return true;
            handle(Minecraft.getInstance(), trimmed);
            return false;
        });
    }

    private static void handle(Minecraft client, String command) {
        String[] args = command.split("\\s+");
        if (args.length == 1 || args[1].equalsIgnoreCase("help")) {
            notify("Commands", ".cdd gui | freecam | fakeplayer | panic | temp");
            return;
        }

        if (args[1].equalsIgnoreCase("gui")) {
            if (args.length == 2) {
                client.gui.setScreen(new DiagnosticsScreen(client.gui.screen()));
                return;
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("bind")) {
                if (CDDKeyMappings.bindGuiToLetter(client, args[3])) {
                    notify("GUI Binding", "Bound to " + args[3].toUpperCase(Locale.ROOT));
                } else {
                    notify("GUI Binding", "Usage: .cdd gui bind <a-z>");
                }
                return;
            }
        }

        if (args[1].equalsIgnoreCase("freecam")) {
            if (args.length == 2) {
                if (CDDSession.isPanicActive()) {
                    notify("Panic is active", "Clear Panic before enabling freecam");
                    return;
                }
                FreecamModule.toggle(client);
                notify("Freecam", FreecamModule.isActive() ? "Enabled" : "Disabled");
                return;
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("speed")) {
                try {
                    FreecamSettings.setSpeed(Double.parseDouble(args[3]));
                    CDDConfigManager.save();
                    notify("Freecam Speed", FreecamSettings.speed() + " blocks/sec");
                } catch (NumberFormatException exception) {
                    notify("Freecam Speed", "Speed must be a number");
                }
                return;
            }
        }

        if (args[1].equalsIgnoreCase("fakeplayer")) {
            if (CDDSession.isPanicActive()) {
                notify("Panic is active", "Clear Panic before spawning Fake Player");
                return;
            }
            FakePlayerModule.toggle(client);
            notify("Fake Player", FakePlayerModule.isSpawned() ? "Spawned locally" : "Removed");
            return;
        }

        if (args[1].equalsIgnoreCase("panic")) {
            CDDSession.togglePanic(client);
            return;
        }

        if (args[1].equalsIgnoreCase("temp")) {
            DiagnosticsSettings.temporaryOverlayMode = !DiagnosticsSettings.temporaryOverlayMode;
            CDDConfigManager.save();
            notify("Temporary Overlay Mode", DiagnosticsSettings.temporaryOverlayMode ? "Enabled" : "Disabled");
            return;
        }

        notify("Command", "Unknown command. Use .cdd help");
    }

    private static void notify(String title, String text) {
        CDDNotifications.show(title, text);
    }
}
