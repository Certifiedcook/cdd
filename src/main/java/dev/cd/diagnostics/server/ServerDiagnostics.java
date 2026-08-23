package dev.cd.diagnostics.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import dev.cd.diagnostics.notification.CDDNotifications;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ServerDiagnostics {
    private static final Set<String> PLATFORM_NAMESPACES = Set.of(
            "minecraft", "bukkit", "spigot", "paper", "fabric", "forge", "neoforge"
    );

    private ServerDiagnostics() {
    }

    public static void showPlugins(Minecraft client) {
        if (client.getConnection() == null) {
            CDDNotifications.show("Server Plugins", "Not connected to a server");
            return;
        }

        CommandDispatcher<?> dispatcher = findDispatcher(client.getConnection());
        if (dispatcher == null) {
            CDDNotifications.show("Server Plugins", "Server command tree is unavailable");
            return;
        }

        Set<String> namespaces = new LinkedHashSet<>();
        for (CommandNode<?> node : dispatcher.getRoot().getChildren()) {
            String name = node.getName();
            int colon = name.indexOf(':');
            if (colon <= 0) continue;

            String namespace = name.substring(0, colon).toLowerCase(Locale.ROOT);
            if (!PLATFORM_NAMESPACES.contains(namespace)) namespaces.add(namespace);
        }

        List<String> detected = new ArrayList<>(namespaces);
        detected.sort(Comparator.naturalOrder());

        if (detected.isEmpty()) {
            CDDNotifications.show(
                    "Server Plugins",
                    "No plugin command namespaces exposed; this does not prove none are installed"
            );
            return;
        }

        int shown = Math.min(8, detected.size());
        String text = String.join(", ", detected.subList(0, shown));
        if (detected.size() > shown) text += " +" + (detected.size() - shown) + " more";
        CDDNotifications.show("Likely Plugins (command tree)", text);
    }

    public static void showTps() {
        if (!ServerTpsEstimator.hasEstimate()) {
            CDDNotifications.show("Server TPS", "Warming up; waiting for server time samples");
            return;
        }

        CDDNotifications.show(
                "Server TPS",
                String.format(Locale.ROOT, "Estimated %.2f TPS (%d samples)",
                        ServerTpsEstimator.estimatedTps(), ServerTpsEstimator.samples())
        );
    }

    private static CommandDispatcher<?> findDispatcher(Object connection) {
        for (String name : List.of("getCommands", "getCommandDispatcher")) {
            try {
                Method method = connection.getClass().getMethod(name);
                Object result = method.invoke(connection);
                if (result instanceof CommandDispatcher<?> dispatcher) return dispatcher;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        for (Method method : connection.getClass().getMethods()) {
            if (!CommandDispatcher.class.isAssignableFrom(method.getReturnType()) || method.getParameterCount() != 0) {
                continue;
            }
            try {
                Object result = method.invoke(connection);
                if (result instanceof CommandDispatcher<?> dispatcher) return dispatcher;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        return null;
    }
}
