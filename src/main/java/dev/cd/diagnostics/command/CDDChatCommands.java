package dev.cd.diagnostics.command;

import dev.cd.diagnostics.config.CDDConfigManager;
import dev.cd.diagnostics.gui.DiagnosticsScreen;
import dev.cd.diagnostics.input.CDDKeyMappings;
import dev.cd.diagnostics.module.FakePlayerModule;
import dev.cd.diagnostics.module.FreecamModule;
import dev.cd.diagnostics.module.FreecamSettings;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

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
            message(client, ".cdd gui | .cdd gui bind <a-z> | .cdd freecam | .cdd freecam speed <1-80> | .cdd fakeplayer");
            return;
        }

        if (args[1].equalsIgnoreCase("gui")) {
            if (args.length == 2) {
                client.gui.setScreen(new DiagnosticsScreen(client.gui.screen()));
                return;
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("bind")) {
                if (CDDKeyMappings.bindGuiToLetter(client, args[3])) {
                    message(client, "GUI bound to " + args[3].toUpperCase(Locale.ROOT));
                } else {
                    message(client, "Usage: .cdd gui bind <a-z>");
                }
                return;
            }
        }

        if (args[1].equalsIgnoreCase("freecam")) {
            if (args.length == 2) {
                FreecamModule.toggle(client);
                message(client, "Freecam: " + (FreecamModule.isActive() ? "ON" : "OFF"));
                return;
            }
            if (args.length == 4 && args[2].equalsIgnoreCase("speed")) {
                try {
                    FreecamSettings.setSpeed(Double.parseDouble(args[3]));
                    CDDConfigManager.save();
                    message(client, "Freecam speed: " + FreecamSettings.speed() + " blocks/sec");
                } catch (NumberFormatException exception) {
                    message(client, "Speed must be a number.");
                }
                return;
            }
        }

        if (args[1].equalsIgnoreCase("fakeplayer")) {
            FakePlayerModule.toggle(client);
            message(client, "Fake player: " + (FakePlayerModule.isSpawned() ? "SPAWNED" : "REMOVED"));
            return;
        }

        message(client, "Unknown command. Use .cdd help");
    }

    private static void message(Minecraft client, String text) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("[CD Diagnostics] " + text));
        }
    }
}
