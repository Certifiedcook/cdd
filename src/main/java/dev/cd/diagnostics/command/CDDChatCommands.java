package dev.cd.diagnostics.command;

import dev.cd.diagnostics.DiagnosticsSettings;
import dev.cd.diagnostics.config.CDDConfigManager;
import dev.cd.diagnostics.gui.DiagnosticsScreen;
import dev.cd.diagnostics.input.CDDKeyMappings;
import dev.cd.diagnostics.module.FakePlayerModule;
import dev.cd.diagnostics.module.FreecamModule;
import dev.cd.diagnostics.module.FreecamSettings;
import dev.cd.diagnostics.module.WalkingModule;
import dev.cd.diagnostics.notification.CDDNotifications;
import dev.cd.diagnostics.server.ServerDiagnostics;
import dev.cd.diagnostics.session.CDDSession;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Locale;

public final class CDDChatCommands {
    private CDDChatCommands() {
    }

    public static void register() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            String trimmed = message.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);

            if (lower.equals(".cdd") || lower.startsWith(".cdd ")) {
                handleCdd(Minecraft.getInstance(), trimmed);
                return false;
            }
            if (lower.equals(".server") || lower.startsWith(".server ")) {
                handleServer(Minecraft.getInstance(), trimmed);
                return false;
            }
            if (lower.equals(";walk") || lower.startsWith(";walk ")) {
                handleWalk(Minecraft.getInstance(), trimmed);
                return false;
            }
            return true;
        });
    }

    private static void handleCdd(Minecraft client, String command) {
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
                if (!FreecamModule.isActive()) WalkingModule.stop(false);
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

    private static void handleServer(Minecraft client, String command) {
        String[] args = command.split("\\s+");
        if (args.length == 1) {
            notify("Server Commands", ".server plugins | .server tps");
            return;
        }

        if (args[1].equalsIgnoreCase("plugins")) {
            ServerDiagnostics.showPlugins(client);
            return;
        }
        if (args[1].equalsIgnoreCase("tps")) {
            ServerDiagnostics.showTps();
            return;
        }

        notify("Server Commands", "Unknown. Use .server plugins or .server tps");
    }

    private static void handleWalk(Minecraft client, String command) {
        String[] args = command.split("\\s+");

        if (args.length == 2 && args[1].equalsIgnoreCase("stop")) {
            WalkingModule.stop(true);
            return;
        }

        if (args.length != 4) {
            notify("Walk", "Usage: ;walk <x> <y> <z> | ;walk stop");
            return;
        }
        if (client.player == null) {
            notify("Walk", "Join a world/server first");
            return;
        }

        try {
            int x = parseCoordinate(args[1], client.player.getX());
            int y = parseCoordinate(args[2], client.player.getY());
            int z = parseCoordinate(args[3], client.player.getZ());
            WalkingModule.start(client, new BlockPos(x, y, z));
        } catch (NumberFormatException exception) {
            notify("Walk", "Coordinates must be numbers or relative values like ~10");
        }
    }

    private static int parseCoordinate(String token, double base) {
        if (token.startsWith("~")) {
            String rest = token.substring(1);
            double offset = rest.isEmpty() ? 0.0 : Double.parseDouble(rest);
            return (int) Math.floor(base + offset);
        }
        return (int) Math.floor(Double.parseDouble(token));
    }

    private static void notify(String title, String text) {
        CDDNotifications.show(title, text);
    }
}
